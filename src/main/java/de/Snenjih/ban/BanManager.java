package de.Snenjih.ban;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.SyncProxy;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.DurationParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class BanManager {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS sp_punishments (
                id           BIGINT       NOT NULL AUTO_INCREMENT,
                uuid         VARCHAR(36)  NULL,
                username     VARCHAR(16)  NOT NULL,
                ip           VARCHAR(45)  NULL,
                type         ENUM('BAN','KICK') NOT NULL,
                reason       TEXT         NOT NULL,
                banner_name  VARCHAR(16)  NOT NULL,
                banner_uuid  VARCHAR(36)  NULL,
                created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                expires_at   DATETIME     NULL,
                active       TINYINT(1)   NOT NULL DEFAULT 1,
                PRIMARY KEY (id),
                INDEX idx_username (username),
                INDEX idx_uuid     (uuid),
                INDEX idx_active   (active)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private final ProxyServer server;
    private final SyncProxy plugin;
    private final ConfigManager config;
    private final DataSource dataSource;
    private final Logger logger;
    private final BanCache cache = new BanCache();

    public BanManager(ProxyServer server, SyncProxy plugin,
                      ConfigManager config, DataSource dataSource, Logger logger) {
        this.server = server;
        this.plugin = plugin;
        this.config = config;
        this.dataSource = dataSource;
        this.logger = logger;
    }

    public void init() {
        initSchema();
        scheduleRefresh();
    }

    private void initSchema() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE);
            logger.info("[BanManager] Tabelle sp_punishments bereit.");
        } catch (SQLException e) {
            logger.error("[BanManager] Fehler beim Erstellen der Tabelle", e);
        }
    }

    private void scheduleRefresh() {
        int interval = config.getInt("ban.cache-refresh-interval", 60);
        server.getScheduler()
                .buildTask(plugin, this::refreshCache)
                .repeat(interval, TimeUnit.SECONDS)
                .schedule();
    }

    private void refreshCache() {
        String sql = "SELECT * FROM sp_punishments WHERE active=1 AND type='BAN' AND (expires_at IS NULL OR expires_at > NOW())";
        Map<String, BanEntry> active = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BanEntry e = mapRow(rs);
                active.put(e.getUsername().toLowerCase(), e);
            }
            cache.replaceAll(active);
        } catch (SQLException e) {
            logger.error("[BanManager] Cache-Refresh fehlgeschlagen", e);
        }
    }

    // ---- Core Operations ----

    public CompletableFuture<BanEntry> ban(String targetUsername, String bannerName,
                                            String bannerUuid, String reason, Instant expiresAt) {
        return CompletableFuture.supplyAsync(() -> {
            // Doppelban verhindern
            Optional<BanEntry> existing = getActiveBanSync(targetUsername);
            if (existing.isPresent()) return null;

            String sql = "INSERT INTO sp_punishments (uuid, username, ip, type, reason, banner_name, banner_uuid, expires_at, active) VALUES (?,?,?,?,?,?,?,?,1)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                // UUID und IP werden beim nächsten Join via backfill nachgetragen
                ps.setNull(1, Types.VARCHAR);
                ps.setString(2, targetUsername);
                ps.setNull(3, Types.VARCHAR);
                ps.setString(4, "BAN");
                ps.setString(5, reason);
                ps.setString(6, bannerName);
                if (bannerUuid != null) ps.setString(7, bannerUuid);
                else ps.setNull(7, Types.VARCHAR);
                if (expiresAt != null) ps.setTimestamp(8, Timestamp.from(expiresAt));
                else ps.setNull(8, Types.TIMESTAMP);
                ps.executeUpdate();

                BanEntry entry;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    long id = keys.getLong(1);
                    entry = new BanEntry(id, null, targetUsername, null, "BAN", reason,
                            bannerName, bannerUuid, Instant.now(), expiresAt, true);
                }
                cache.put(targetUsername, entry);
                kickOnlinePlayer(targetUsername, entry);
                return entry;
            } catch (SQLException e) {
                logger.error("[BanManager] Ban-Insert fehlgeschlagen", e);
                return null;
            }
        });
    }

    public CompletableFuture<Boolean> pardon(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE sp_punishments SET active=0 WHERE LOWER(username)=LOWER(?) AND active=1 AND type='BAN'";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                int rows = ps.executeUpdate();
                cache.invalidate(username);
                return rows > 0;
            } catch (SQLException e) {
                logger.error("[BanManager] Pardon fehlgeschlagen", e);
                return false;
            }
        });
    }

    public CompletableFuture<Optional<BanEntry>> getActiveBan(String username) {
        return CompletableFuture.supplyAsync(() -> getActiveBanSync(username));
    }

    private Optional<BanEntry> getActiveBanSync(String username) {
        Optional<BanEntry> cached = cache.get(username);
        if (cached != null) return cached;

        String sql = "SELECT * FROM sp_punishments WHERE LOWER(username)=LOWER(?) AND active=1 AND type='BAN' AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BanEntry e = mapRow(rs);
                    cache.put(username, e);
                    return Optional.of(e);
                }
            }
        } catch (SQLException e) {
            logger.error("[BanManager] getActiveBan fehlgeschlagen", e);
        }
        cache.invalidate(username);
        return Optional.empty();
    }

    /** Synchron nur aus dem Cache lesen (null = Cache-Miss). */
    public Optional<BanEntry> getActiveBanCached(String username) {
        return cache.get(username);
    }

    public CompletableFuture<List<BanEntry>> getHistory(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM sp_punishments WHERE LOWER(username)=LOWER(?) ORDER BY created_at DESC";
            List<BanEntry> list = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapRow(rs));
                }
            } catch (SQLException e) {
                logger.error("[BanManager] getHistory fehlgeschlagen", e);
            }
            return list;
        });
    }

    public CompletableFuture<Integer> clearHistory(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM sp_punishments WHERE LOWER(username)=LOWER(?) AND active=0";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                return ps.executeUpdate();
            } catch (SQLException e) {
                logger.error("[BanManager] clearHistory fehlgeschlagen", e);
                return 0;
            }
        });
    }

    public CompletableFuture<BanEntry> logKick(String targetUsername, String bannerName,
                                                String bannerUuid, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO sp_punishments (uuid, username, ip, type, reason, banner_name, banner_uuid, expires_at, active) VALUES (?,?,?,?,?,?,?,NULL,0)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setNull(1, Types.VARCHAR);
                ps.setString(2, targetUsername);
                ps.setNull(3, Types.VARCHAR);
                ps.setString(4, "KICK");
                ps.setString(5, reason);
                ps.setString(6, bannerName);
                if (bannerUuid != null) ps.setString(7, bannerUuid);
                else ps.setNull(7, Types.VARCHAR);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    long id = keys.getLong(1);
                    return new BanEntry(id, null, targetUsername, null, "KICK", reason,
                            bannerName, bannerUuid, Instant.now(), null, false);
                }
            } catch (SQLException e) {
                logger.error("[BanManager] logKick fehlgeschlagen", e);
                return null;
            }
        });
    }

    /** Trägt UUID und IP nach für Rows, die noch NULL haben (Offline-Bans). */
    public void backfillPlayerData(String uuid, String username, String ip) {
        server.getScheduler().buildTask(plugin, () -> {
            String sql = "UPDATE sp_punishments SET uuid=?, ip=? WHERE LOWER(username)=LOWER(?) AND uuid IS NULL";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid);
                ps.setString(2, ip);
                ps.setString(3, username);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.error("[BanManager] Backfill fehlgeschlagen", e);
            }
        }).schedule();
    }

    /** Wenn ip-ban-cascade aktiv: alle Online-Spieler mit gleicher IP bannen. */
    public void triggerIpCascade(String ip, String triggerUuid) {
        if (!config.getBoolean("ip-ban-cascade.enabled", false) || ip == null) return;

        String durationStr = config.getString("ip-ban-cascade.duration", "30d");
        String reason = config.getString("ip-ban-cascade.reason", "Verbunden mit einem gebannten Spieler");

        Optional<Instant> expiresAt;
        try {
            expiresAt = DurationParser.parse(durationStr);
        } catch (IllegalArgumentException e) {
            expiresAt = Optional.of(Instant.now().plusSeconds(30L * 86400));
        }

        final Instant finalExpiry = expiresAt.orElse(null);

        for (Player player : server.getAllPlayers()) {
            String playerUuid = player.getUniqueId().toString();
            if (playerUuid.equals(triggerUuid)) continue;

            player.getRemoteAddress().getAddress().getHostAddress();
            String playerIp = player.getRemoteAddress().getAddress().getHostAddress();
            if (!playerIp.equals(ip)) continue;

            Optional<BanEntry> existing = getActiveBanSync(player.getUsername());
            if (existing.isPresent()) continue;

            ban(player.getUsername(), "System", null, reason, finalExpiry);
        }
    }

    public String resolvePlaceholders(String template, BanEntry ban) {
        String result = template
                .replace("{player}",    ban.getUsername())
                .replace("{banner}",    ban.getBannerName())
                .replace("{reason}",    ban.getReason())
                .replace("{banned_at}", DATE_FMT.format(ban.getCreatedAt()));

        if (ban.isPermanent()) {
            result = result
                    .replace("{expires_at}",     "Permanent")
                    .replace("{time_remaining}", "Permanent");
        } else {
            result = result
                    .replace("{expires_at}",     DATE_FMT.format(ban.getExpiresAt()))
                    .replace("{time_remaining}", DurationParser.humanize(ban.getExpiresAt()));
        }
        return result;
    }

    private void kickOnlinePlayer(String username, BanEntry ban) {
        server.getPlayer(username).ifPresent(player -> {
            String template = config.getString("ban.ban-screen-message",
                    "§cDu wurdest gebannt.\n§7Grund: §c{reason}");
            String msg = resolvePlaceholders(template, ban);
            Component component = LegacyComponentSerializer.legacySection().deserialize(msg);
            player.disconnect(component);
        });
    }

    private BanEntry mapRow(ResultSet rs) throws SQLException {
        Timestamp expiresTs = rs.getTimestamp("expires_at");
        Instant expiresAt = expiresTs != null ? expiresTs.toInstant() : null;
        return new BanEntry(
                rs.getLong("id"),
                rs.getString("uuid"),
                rs.getString("username"),
                rs.getString("ip"),
                rs.getString("type"),
                rs.getString("reason"),
                rs.getString("banner_name"),
                rs.getString("banner_uuid"),
                rs.getTimestamp("created_at").toInstant(),
                expiresAt,
                rs.getInt("active") == 1
        );
    }
}
