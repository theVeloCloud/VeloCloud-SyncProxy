package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.ban.BanEntry;
import de.Snenjih.ban.BanManager;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.DurationParser;
import de.Snenjih.util.Messages;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CheckCommand implements SimpleCommand {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final ProxyServer server;
    private final ConfigManager config;
    private final BanManager banManager;

    public CheckCommand(ProxyServer server, ConfigManager config, BanManager banManager) {
        this.server = server;
        this.config = config;
        this.banManager = banManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("syncproxy.command.check")) {
            source.sendMessage(Messages.noPermission());
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 1) {
            source.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§7Nutzung: §b/check §8(§7Spieler§8) §8[§7--reset§8]")));
            return;
        }

        String target = args[0];
        boolean reset = Arrays.asList(args).contains("--reset");

        server.getScheduler().buildTask(this, () -> {
            List<BanEntry> history = banManager.getHistory(target).join();

            if (reset) {
                int deleted = banManager.clearHistory(target).join();
                source.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§7History von §b" + target + " §7gelöscht. §8(§7" + deleted + " Einträge§8)")));
            }

            source.sendMessage(Messages.legacy("§8§m──────────────────────────────"));
            source.sendMessage(Messages.legacy("§b§lCheck §8▪ §7" + target));
            source.sendMessage(Messages.legacy("§8§m──────────────────────────────"));

            if (history.isEmpty()) {
                source.sendMessage(Messages.legacy("  §7Keine Einträge gefunden."));
            } else {
                for (BanEntry e : history) {
                    String type = e.getType().equals("BAN") ? "§cBAN" : "§eKICK";
                    String status = e.isCurrentlyBanned() ? " §a[AKTIV]" : "";
                    String expires = e.getType().equals("BAN")
                            ? (e.isPermanent() ? "§cPermanent" : DATE_FMT.format(e.getExpiresAt()))
                            : "§8-";
                    source.sendMessage(Messages.legacy(
                            "  " + type + status + " §8| §7" + DATE_FMT.format(e.getCreatedAt())
                            + " §8| §7Von: §b" + e.getBannerName()
                            + " §8| §7Bis: §b" + expires));
                    source.sendMessage(Messages.legacy(
                            "    §8» §7Grund: §f" + e.getReason()));
                }
            }
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
                            .map(p -> p.getUsername())
                            .filter(n -> n.toLowerCase().startsWith(input))
                            .toList());
        }
        if (args.length == 2) {
            return CompletableFuture.completedFuture(List.of("--reset"));
        }
        return CompletableFuture.completedFuture(List.of());
    }
}
