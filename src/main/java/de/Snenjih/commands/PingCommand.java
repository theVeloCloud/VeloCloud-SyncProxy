package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.Messages;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PingCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;

    public PingCommand(ProxyServer server, ConfigManager config) {
        this.server = server;
        this.config = config;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // /ping — eigener Ping
        if (args.length == 0) {
            if (!(source instanceof Player self)) {
                source.sendMessage(Messages.legacy("§cAls Konsole musst du einen Spielernamen angeben: §b/ping <Spieler>"));
                return;
            }
            long ping = self.getPing();
            source.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§7Dein Ping: " + pingColor(ping) + ping + " ms")));
            return;
        }

        // /ping <player> — Permission erforderlich
        if (!source.hasPermission("syncproxy.command.ping.others")) {
            source.sendMessage(Messages.noPermission());
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
        long ping = target.getPing();
        source.sendMessage(Messages.prefix(config).append(
                Messages.legacy("§7Ping von §b" + target.getUsername() + "§7: " + pingColor(ping) + ping + " ms")));
    }

    private String pingColor(long ping) {
        if (ping < 50) return "§a";
        if (ping < 100) return "§e";
        if (ping < 200) return "§6";
        return "§c";
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1 && invocation.source().hasPermission("syncproxy.command.ping.others")) {
            String input = args[0].toLowerCase();
            return CompletableFuture.completedFuture(
                    server.getAllPlayers().stream()
                            .map(Player::getUsername)
                            .filter(n -> n.toLowerCase().startsWith(input))
                            .toList());
        }
        return CompletableFuture.completedFuture(List.of());
    }
}
