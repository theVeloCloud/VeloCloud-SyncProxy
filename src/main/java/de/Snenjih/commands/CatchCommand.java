package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.Messages;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CatchCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;

    public CatchCommand(ProxyServer server, ConfigManager config) {
        this.server = server;
        this.config = config;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("syncproxy.command.catch")) {
            source.sendMessage(Messages.noPermission());
            return;
        }

        if (!(source instanceof Player sender)) {
            source.sendMessage(Messages.legacy("§cDieser Command kann nur von Spielern ausgeführt werden."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 1) {
            sender.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§7Nutzung: §b/catch §8(§7Spieler§8)")));
            return;
        }

        Optional<RegisteredServer> senderServerOpt = sender.getCurrentServer()
                .map(conn -> conn.getServer());
        if (senderServerOpt.isEmpty()) {
            sender.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§cDu bist auf keinem Server verbunden.")));
            return;
        }
        RegisteredServer senderServer = senderServerOpt.get();

        String targetName = args[0];

        if (targetName.equalsIgnoreCase(sender.getUsername())) {
            sender.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§cDu kannst dich nicht selbst catchen.")));
            return;
        }

        Optional<Player> targetOpt = server.getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            sender.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§c" + targetName + " §7ist nicht online.")));
            return;
        }

        Player target = targetOpt.get();

        // Already on the same server
        boolean alreadyHere = target.getCurrentServer()
                .map(conn -> conn.getServer().getServerInfo().getName()
                        .equals(senderServer.getServerInfo().getName()))
                .orElse(false);
        if (alreadyHere) {
            sender.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§b" + target.getUsername() + " §7ist bereits auf deinem Server.")));
            return;
        }

        target.createConnectionRequest(senderServer).fireAndForget();
        target.sendMessage(Messages.prefix(config).append(
                Messages.legacy("§7Du wurdest von §b" + sender.getUsername() + " §7zu Server §b" + senderServer.getServerInfo().getName() + " §7gezogen§8!")));
        sender.sendMessage(Messages.prefix(config).append(
                Messages.legacy("§7Du hast §b" + target.getUsername() + " §7zu dir gezogen§8!")));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            String senderName = invocation.source() instanceof Player p ? p.getUsername() : null;
            return CompletableFuture.completedFuture(
                    server.getAllPlayers().stream()
                            .map(Player::getUsername)
                            .filter(n -> !n.equals(senderName))
                            .filter(n -> n.toLowerCase().startsWith(input))
                            .toList());
        }
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("syncproxy.command.catch");
    }
}
