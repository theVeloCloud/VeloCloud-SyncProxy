package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.Messages;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ServerCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;

    public ServerCommand(ProxyServer server, ConfigManager config) {
        this.server = server;
        this.config = config;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player player)) {
            source.sendMessage(Messages.legacy("§cDieser Command kann nur von Spielern verwendet werden."));
            return;
        }
        if (!player.hasPermission("snenjih.command.server")) {
            player.sendMessage(Messages.noPermission());
            return;
        }

        String[] args = invocation.arguments();
        if (args.length != 1) {
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(" ")));
            player.sendMessage(Messages.prefix(config).append(Messages.legacy("§b§lServer §8▪ §7Verwendung")));
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(" ")));
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(" §8» §7/server §8(§7server§8)")));
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(" ")));
            return;
        }

        final String serverName = args[0];
        Optional<RegisteredServer> targetOpt = server.getServer(serverName);
        if (targetOpt.isEmpty()) {
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(
                    "§7Der Server §c" + serverName + " §7konnte nicht gefunden werden§8!")));
            return;
        }

        RegisteredServer target = targetOpt.get();
        boolean alreadyConnected = player.getCurrentServer()
                .map(conn -> conn.getServerInfo().getName().equals(serverName))
                .orElse(false);

        if (alreadyConnected) {
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(
                    "§7Du bist bereits auf §b" + serverName + " §7verbunden§8!")));
            return;
        }

        player.createConnectionRequest(target).connectWithIndication();
        player.sendMessage(Messages.prefix(config).append(Messages.legacy(
                "§7Du wirst zu §b" + serverName + " §7verbunden§8.")));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        if (invocation.arguments().length != 1) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        String input = invocation.arguments()[0].toLowerCase();
        List<String> suggestions = server.getAllServers().stream()
                .map(s -> s.getServerInfo().getName())
                .filter(n -> n.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
        return CompletableFuture.completedFuture(suggestions);
    }
}
