package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.Messages;
import de.snenjih.velocloud.shared.VelocloudSharedKt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CstartCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;

    public CstartCommand(ProxyServer server, ConfigManager config) {
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
        if (args.length != 2) {
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(" ")));
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(
                    "§b§lCstart §8▪ §7Seite 1 §8- §7/cstart §8(§71§8)")));
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(" ")));
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(
                    " §8» §7/cstart §8(§7server§8) §8(§7Anzahl§8)")));
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(" ")));
            return;
        }

        final String group = args[0];
        final int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(
                    "§7Du hast keine richtige Zahl angegeben§8! Nutze: §7/cstart §8(§7server§8) §8(§7Anzahl§8)")));
            return;
        }

        server.getScheduler().buildTask(this, () -> {
            var serviceProvider = VelocloudSharedKt.getVelocloudShared().serviceProvider();
            for (int i = 0; i < amount; i++) {
                serviceProvider.bootInstance(group);
            }
            player.sendMessage(Messages.prefix(config).append(Messages.legacy(
                    "§7Du hast §a" + amount + "x §7" + group + " §aServer gestartet§8.")));
        }).schedule();
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            var serviceProvider = VelocloudSharedKt.getVelocloudShared().serviceProvider();
            return serviceProvider.findAllAsync().thenApply(services -> {
                List<String> groups = new ArrayList<>();
                for (var service : services) {
                    if (!groups.contains(service.getGroupName()) && service.getGroupName().toLowerCase().startsWith(input)) {
                        groups.add(service.getGroupName());
                    }
                }
                return groups;
            });
        }
        if (args.length == 2) {
            List<String> suggestions = new ArrayList<>();
            for (int i = 1; i <= 5; i++) suggestions.add(String.valueOf(i));
            return CompletableFuture.completedFuture(suggestions);
        }
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
}
