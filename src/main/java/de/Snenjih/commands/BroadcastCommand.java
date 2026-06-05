package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BroadcastCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;

    public BroadcastCommand(ProxyServer server, ConfigManager config) {
        this.server = server;
        this.config = config;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("syncproxy.command.broadcast")) {
            source.sendMessage(Messages.noPermission());
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 1) {
            source.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§7Nutzung: §b/broadcast §8(§7Nachricht§8)")));
            return;
        }

        String rawMessage = String.join(" ", args);
        String broadcastPrefix = config.getString("broadcast-prefix", "§8[§6Broadcast§8] §r");
        Component message = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(broadcastPrefix + rawMessage);

        server.sendMessage(Component.empty());
        server.sendMessage(Component.empty());
        server.sendMessage(message);
        server.sendMessage(Component.empty());
        server.sendMessage(Component.empty());
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("syncproxy.command.broadcast");
    }
}
