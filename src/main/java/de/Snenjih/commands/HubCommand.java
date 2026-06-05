package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.LobbyRouter;
import de.Snenjih.util.Messages;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class HubCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;

    public HubCommand(ProxyServer server, ConfigManager config) {
        this.server = server;
        this.config = config;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        Player target;
        if (args.length >= 1) {
            if (!source.hasPermission("syncproxy.command.hub.others")) {
                source.sendMessage(Messages.noPermission());
                return;
            }
            Optional<Player> targetOpt = server.getPlayer(args[0]);
            if (targetOpt.isEmpty()) {
                source.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§c" + args[0] + " §7ist nicht online.")));
                return;
            }
            target = targetOpt.get();
        } else {
            if (!(source instanceof Player p)) {
                source.sendMessage(Messages.legacy("§cNur Spieler können /hub ohne Argument verwenden."));
                return;
            }
            target = p;
        }

        final Player finalTarget = target;
        server.getScheduler().buildTask(this, () -> {
            String groupName = config.getString("hub.fallback-group", "Lobby");
            RegisteredServer lobby = LobbyRouter.pickLeastFull(server, groupName);

            if (lobby == null) {
                String staticFallback = config.getString("fallback-server", "Lobby-1");
                lobby = server.getServer(staticFallback).orElse(null);
            }

            if (lobby == null) {
                finalTarget.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§cKein Lobby-Server verfügbar.")));
                return;
            }

            // Check if already on this server
            final RegisteredServer finalLobby = lobby;
            boolean alreadyThere = finalTarget.getCurrentServer()
                    .map(s -> s.getServerInfo().getName().equals(finalLobby.getServerInfo().getName()))
                    .orElse(false);

            if (alreadyThere) {
                finalTarget.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§7Du bist bereits auf dem Lobby-Server.")));
                return;
            }

            finalTarget.createConnectionRequest(finalLobby).connectWithIndication();
            finalTarget.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§7Du wirst zur Lobby gesendet§8.")));

            if (source != finalTarget) {
                source.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§7" + finalTarget.getUsername() + " §7wurde zur Lobby gesendet§8.")));
            }
        }).schedule();
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1 && invocation.source().hasPermission("syncproxy.command.hub.others")) {
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
