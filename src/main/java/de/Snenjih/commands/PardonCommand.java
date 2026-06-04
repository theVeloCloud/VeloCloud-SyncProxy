package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.ban.BanManager;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.Messages;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PardonCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;
    private final BanManager banManager;

    public PardonCommand(ProxyServer server, ConfigManager config, BanManager banManager) {
        this.server = server;
        this.config = config;
        this.banManager = banManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("syncproxy.command.pardon")) {
            source.sendMessage(Messages.noPermission());
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 1) {
            source.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§7Nutzung: §b/pardon §8(§7Spieler§8)")));
            return;
        }

        String target = args[0];
        server.getScheduler().buildTask(this, () -> {
            boolean wasActive = banManager.pardon(target).join();
            if (wasActive) {
                source.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§7Spieler §a" + target + " §7wurde entbannt.")));
            } else {
                source.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§c" + target + " §7ist nicht gebannt.")));
            }
        }).schedule();
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
}
