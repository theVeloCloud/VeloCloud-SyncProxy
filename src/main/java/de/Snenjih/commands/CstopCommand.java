package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.Messages;
import de.snenjih.velocloud.shared.VelocloudSharedKt;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CstopCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;

    public CstopCommand(ProxyServer server, ConfigManager config) {
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
        if (!player.hasPermission("cloudnet.command.service")) {
            player.sendMessage(Messages.noPermission());
            return;
        }

        String[] args = invocation.arguments();
        if (args.length != 1) {
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(
                    "§7Bitte nutze: §b/cstop §8(§7Server§8)")));
            return;
        }

        final String serverName = args[0];
        Optional<RegisteredServer> targetOpt = server.getServer(serverName);
        if (targetOpt.isEmpty()) {
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(
                    "§7Der Server §c" + serverName + " §7wurde nicht gefunden§8!")));
            return;
        }

        server.getScheduler().buildTask(this, () -> {
            RegisteredServer target = targetOpt.get();

            if (serverName.startsWith("Lobby") || serverName.startsWith("lobby")) {
                String disconnectMsg = config.getString("disconnect-message",
                        "§7Der Server wurde gestoppt oder restartet.");
                Component disconnectComponent = Messages.legacy(disconnectMsg);
                for (Player onTarget : target.getPlayersConnected()) {
                    onTarget.disconnect(disconnectComponent);
                }
            } else {
                String fallbackName = config.getString("fallback-server", "Lobby-1");
                Optional<RegisteredServer> fallbackOpt = server.getServer(fallbackName);
                if (fallbackOpt.isEmpty()) {
                    player.sendMessage(Messages.prefix(config).append(Messages.legacy(
                            "§cFallback-Server §7'" + fallbackName + "' §cnicht verfügbar§8!")));
                    return;
                }
                RegisteredServer fallback = fallbackOpt.get();
                for (Player onTarget : target.getPlayersConnected()) {
                    onTarget.createConnectionRequest(fallback).connectWithIndication();
                    onTarget.sendMessage(Messages.legacy(
                            "§7Der Server wurde gestoppt oder restartet. Du wirst weitergeleitet§8."));
                }
            }

            VelocloudSharedKt.getVelocloudShared().serviceProvider().shutdownService(serverName);
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(
                    "§7Du hast den Server §b" + serverName + " §7gestoppt§8.")));
        }).schedule();
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
