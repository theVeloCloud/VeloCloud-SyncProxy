package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.ban.BanEntry;
import de.Snenjih.ban.BanManager;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.Messages;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class InfoCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;
    private final BanManager banManager;

    public InfoCommand(ProxyServer server, ConfigManager config, BanManager banManager) {
        this.server = server;
        this.config = config;
        this.banManager = banManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("syncproxy.command.info")) {
            source.sendMessage(Messages.noPermission());
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 1) {
            source.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§7Verwendung: §b/info <Spieler>")));
            return;
        }

        String targetName = args[0];
        Optional<Player> targetOpt = server.getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            source.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§c" + targetName + " §7ist nicht online.")));
            return;
        }

        Player target = targetOpt.get();
        UUID targetUuid = target.getUniqueId();

        server.getScheduler().buildTask(this, () -> {
            // Re-check in case player disconnected during async execution
            if (server.getPlayer(targetUuid).isEmpty()) {
                source.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§c" + targetName + " §7hat die Verbindung getrennt.")));
                return;
            }

            String uuid = targetUuid.toString();
            String ip = target.getRemoteAddress().getAddress().getHostAddress();
            String hashedIp = sha256Hex(ip);
            String currentServer = target.getCurrentServer()
                    .map(s -> s.getServerInfo().getName()).orElse("§7?");
            long ping = target.getPing();

            Optional<BanEntry> ban = banManager.getActiveBan(target.getUsername()).join();
            String banStatus = ban.isPresent()
                    ? "§cGebannt §8(§7" + ban.get().getReason() + "§8)"
                    : "§aNicht gebannt";

            source.sendMessage(Messages.legacy("§8§m──────────────────────────────"));
            source.sendMessage(Messages.legacy("§b§lInfo §8▪ §7" + target.getUsername()));
            source.sendMessage(Messages.legacy("§8§m──────────────────────────────"));
            source.sendMessage(Messages.legacy("  §7UUID: §b" + uuid));
            source.sendMessage(Messages.legacy("  §7IP-Hash: §b" + hashedIp));
            source.sendMessage(Messages.legacy("  §7Server: §b" + currentServer));
            source.sendMessage(Messages.legacy("  §7Ping: §b" + ping + "ms"));
            source.sendMessage(Messages.legacy("  §7Banstatus: " + banStatus));
            source.sendMessage(Messages.legacy("§8§m──────────────────────────────"));
        }).schedule();
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return CompletableFuture.completedFuture(
                    server.getAllPlayers().stream()
                            .map(Player::getUsername)
                            .filter(n -> n.toLowerCase().startsWith(input))
                            .toList());
        }
        return CompletableFuture.completedFuture(List.of());
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            return "????????";
        }
    }
}
