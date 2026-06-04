package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.ban.BanEntry;
import de.Snenjih.ban.BanManager;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.DurationParser;
import de.Snenjih.util.Messages;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PunishCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;
    private final BanManager banManager;

    public PunishCommand(ProxyServer server, ConfigManager config, BanManager banManager) {
        this.server = server;
        this.config = config;
        this.banManager = banManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("syncproxy.command.punish")) {
            source.sendMessage(Messages.noPermission());
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 3) {
            sendUsage(source);
            return;
        }

        String target = args[0];

        DurationParser.ParseResult parsed;
        try {
            parsed = DurationParser.parseFromArgs(args, 1);
        } catch (IllegalArgumentException e) {
            source.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§cUngültige Dauer. Beispiele: §71d§8, §720m§8, §71d12h§8, §7perma")));
            return;
        }

        if (parsed.reasonStartIndex() >= args.length) {
            sendUsage(source);
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, parsed.reasonStartIndex(), args.length));
        Instant expiresAt = parsed.expiresAt().orElse(null);

        String bannerName = source instanceof com.velocitypowered.api.proxy.Player p
                ? p.getUsername() : "CONSOLE";
        String bannerUuid = source instanceof com.velocitypowered.api.proxy.Player p
                ? p.getUniqueId().toString() : null;

        server.getScheduler().buildTask(this, () -> {
            // Bereits gebannt?
            Optional<BanEntry> existing = banManager.getActiveBan(target).join();
            if (existing.isPresent()) {
                source.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§c" + target + " §7ist bereits gebannt.")));
                return;
            }

            BanEntry entry = banManager.ban(target, bannerName, bannerUuid, reason, expiresAt).join();
            if (entry != null) {
                String durationDisplay = expiresAt == null ? "Permanent"
                        : DurationParser.humanize(expiresAt);
                source.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§7Spieler §c" + target + " §7wurde für §c" + durationDisplay
                                + " §7gebannt. §8(§7Grund: §c" + reason + "§8)")));
            } else {
                source.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§cFehler beim Bannen. Überprüfe die Logs.")));
            }
        }).schedule();
    }

    private void sendUsage(CommandSource source) {
        source.sendMessage(Messages.prefix(config).append(Messages.legacy(" ")));
        source.sendMessage(Messages.prefix(config).append(Messages.legacy(
                "§b§lPunish §8▪ §7/punish §8(§7Spieler§8) §8(§7Dauer§8) §8(§7Grund§8)")));
        source.sendMessage(Messages.prefix(config).append(Messages.legacy(" ")));
        source.sendMessage(Messages.prefix(config).append(Messages.legacy(
                " §8» §7Dauer-Beispiele: §71d§8, §720m§8, §730s§8, §71d12h§8, §7perma")));
        source.sendMessage(Messages.prefix(config).append(Messages.legacy(" ")));
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
            return CompletableFuture.completedFuture(
                    List.of("perma", "1d", "7d", "30d", "1h", "30m"));
        }
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
}
