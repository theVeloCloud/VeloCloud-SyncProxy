package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.Messages;
import de.snenjih.velocloud.shared.VelocloudSharedKt;
import de.snenjih.velocloud.shared.service.SharedServiceProvider;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CrestartCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;

    public CrestartCommand(ProxyServer server, ConfigManager config) {
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
        if (!player.hasPermission("velocloud.command.service")) {
            player.sendMessage(Messages.noPermission());
            return;
        }

        String[] args = invocation.arguments();
        final String serverName;

        if (args.length == 0) {
            Optional<String> currentServer = player.getCurrentServer()
                    .map(conn -> conn.getServerInfo().getName());
            if (currentServer.isEmpty()) {
                player.sendMessage(Messages.prefix(config).append(Messages.legacy(
                        "§7Du bist auf keinem Server§8!")));
                return;
            }
            serverName = currentServer.get();
        } else {
            serverName = args[0];
        }

        server.getScheduler().buildTask(this, () -> {
            SharedServiceProvider<?> serviceProvider = VelocloudSharedKt.getVelocloudShared().serviceProvider();
            var service = serviceProvider.find(serverName);
            if (service == null) {
                player.sendMessage(Messages.prefix(config).append(Messages.legacy(
                        "§7Der Service §c" + serverName + " §7wurde nicht gefunden§8!")));
                return;
            }
            String groupName = service.getGroupName();
            serviceProvider.shutdownService(serverName);
            serviceProvider.bootInstance(groupName);
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(
                    "§7Du hast den Server §b" + serverName + " §7neugestartet§8.")));
        }).schedule();
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        if (invocation.arguments().length > 1) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        String input = invocation.arguments().length == 1 ? invocation.arguments()[0].toLowerCase() : "";
        List<String> suggestions = server.getAllServers().stream()
                .map(s -> s.getServerInfo().getName())
                .filter(n -> n.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
        return CompletableFuture.completedFuture(suggestions);
    }
}
