package de.Snenjih.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.ban.BanEntry;
import de.Snenjih.ban.BanManager;
import de.Snenjih.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Optional;
import java.util.UUID;

public class LoginListener {

    private final ProxyServer server;
    private final BanManager banManager;
    private final ConfigManager config;

    public LoginListener(ProxyServer server, BanManager banManager, ConfigManager config) {
        this.server = server;
        this.banManager = banManager;
        this.config = config;
    }

    @Subscribe(async = true)
    public void onPreLogin(PreLoginEvent event) {
        String username = event.getUsername();
        String ip = event.getConnection().getRemoteAddress().getAddress().getHostAddress();

        // UUID nur wenn verfügbar (online-mode), sonst null
        UUID uuidObj = event.getUniqueId();
        String uuid = uuidObj != null ? uuidObj.toString() : null;

        // UUID und IP für Offline-Bans nachpflegen (nur wenn UUID bekannt)
        if (uuid != null) {
            banManager.backfillPlayerData(uuid, username, ip);
        }

        // Ban-Prüfung (blockierend, da wir auf async-Thread sind)
        Optional<BanEntry> ban = banManager.getActiveBan(username).join();
        if (ban.isPresent()) {
            BanEntry entry = ban.get();
            String template = config.getString("ban.ban-screen-message",
                    "§cDu wurdest gebannt.\n§7Grund: §c{reason}");
            String msg = banManager.resolvePlaceholders(template, entry);
            Component component = LegacyComponentSerializer.legacySection().deserialize(msg);
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(component));
            return;
        }

        // IP-Cascade prüfen (nur wenn UUID bekannt um Selbst-Ban zu verhindern)
        if (uuid != null) {
            banManager.triggerIpCascade(ip, uuid);
        }
    }
}
