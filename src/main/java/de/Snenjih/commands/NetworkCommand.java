package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.Messages;
import de.snenjih.velocloud.sdk.java.Velocloud;
import de.snenjih.velocloud.shared.service.Service;
import de.snenjih.velocloud.v1.services.ServiceState;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NetworkCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;

    public NetworkCommand(ProxyServer server, ConfigManager config) {
        this.server = server;
        this.config = config;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("syncproxy.command.network")) {
            source.sendMessage(Messages.noPermission());
            return;
        }

        server.getScheduler().buildTask(this, () -> {
            List<Service> services;
            try {
                services = Velocloud.instance().serviceProvider().findAll();
            } catch (Exception e) {
                source.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§cFehler beim Abrufen der Server-Übersicht.")));
                return;
            }

            if (services.isEmpty()) {
                source.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§7Keine Server gefunden.")));
                return;
            }

            services = services.stream()
                    .sorted(Comparator.comparing(Service::getGroupName).thenComparing(Service::name))
                    .toList();

            source.sendMessage(Messages.legacy("§8§m──────────────────────────────"));
            source.sendMessage(Messages.legacy("§b§lNetwork §8▪ §7Server-Übersicht"));
            source.sendMessage(Messages.legacy("§8§m──────────────────────────────"));

            String lastGroup = null;
            for (Service svc : services) {
                if (!svc.getGroupName().equals(lastGroup)) {
                    lastGroup = svc.getGroupName();
                    source.sendMessage(Messages.legacy(" §e§l" + lastGroup));
                }

                boolean online = svc.getState() == ServiceState.ONLINE;
                String stateColor = online ? "§a" : "§c";
                String stateLabel = online ? "ONLINE" : svc.getState().name();

                int playerCount = server.getServer(svc.name())
                        .map(rs -> rs.getPlayersConnected().size())
                        .orElse(svc.getPlayerCount());

                source.sendMessage(Messages.legacy(
                        "  §8» " + stateColor + svc.name()
                        + " §8[§7" + playerCount + "§8] "
                        + stateColor + "[" + stateLabel + "]"));
            }
            source.sendMessage(Messages.legacy("§8§m──────────────────────────────"));
        }).schedule();
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(List.of());
    }
}
