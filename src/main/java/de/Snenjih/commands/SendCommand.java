package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.Messages;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SendCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;

    public SendCommand(ProxyServer server, ConfigManager config) {
        this.server = server;
        this.config = config;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("syncproxy.command.send")) {
            source.sendMessage(Messages.noPermission());
            return;
        }

        if (!(source instanceof Player sender)) {
            source.sendMessage(Messages.legacy("§cDieser Command kann nur von Spielern ausgeführt werden."));
            return;
        }

        String[] args = invocation.arguments();

        // /send <player|all|server> — pull to sender's current server
        if (args.length == 1) {
            Optional<RegisteredServer> senderServerOpt = sender.getCurrentServer()
                    .map(conn -> conn.getServer());
            if (senderServerOpt.isEmpty()) {
                sender.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§cDu bist auf keinem Server verbunden.")));
                return;
            }
            RegisteredServer senderServer = senderServerOpt.get();
            String target = args[0];

            if (target.equalsIgnoreCase("all")) {
                for (Player p : server.getAllPlayers()) {
                    connectAndNotify(p, senderServer, sender);
                }
                sender.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§7Du hast §balle Spieler §7auf deinen Server §b" + senderServer.getServerInfo().getName() + " §7gesendet§8!")));
            } else {
                Optional<Player> targetPlayer = server.getPlayer(target);
                if (targetPlayer.isPresent()) {
                    connectAndNotify(targetPlayer.get(), senderServer, sender);
                    sender.sendMessage(Messages.prefix(config).append(
                            Messages.legacy("§7Du hast §b" + targetPlayer.get().getUsername() + " §7auf deinen Server §b" + senderServer.getServerInfo().getName() + " §7gesendet§8!")));
                } else {
                    Optional<RegisteredServer> targetServer = server.getServer(target);
                    if (targetServer.isPresent()) {
                        Collection<Player> playersOnServer = targetServer.get().getPlayersConnected();
                        for (Player p : playersOnServer) {
                            connectAndNotify(p, senderServer, sender);
                        }
                        sender.sendMessage(Messages.prefix(config).append(
                                Messages.legacy("§7Du hast §balle Spieler von Server §b" + target + " §7auf deinen Server §b" + senderServer.getServerInfo().getName() + " §7gesendet§8!")));
                    } else {
                        sender.sendMessage(Messages.prefix(config).append(
                                Messages.legacy("§cDer Spieler/Server §e" + target + " §7wurde nicht gefunden§8!")));
                    }
                }
            }
            return;
        }

        // /send <player|all|server> <targetServer>
        if (args.length == 2) {
            String choose = args[0];
            Optional<RegisteredServer> targetServerOpt = server.getServer(args[1]);
            if (targetServerOpt.isEmpty()) {
                sender.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§cDer Ziel-Server §e" + args[1] + " §7wurde nicht gefunden§8!")));
                return;
            }
            RegisteredServer targetServer = targetServerOpt.get();

            if (choose.equalsIgnoreCase("all")) {
                for (Player p : server.getAllPlayers()) {
                    connectAndNotify(p, targetServer, sender);
                }
                sender.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§7Du hast §balle Spieler §7auf den Server §b" + targetServer.getServerInfo().getName() + " §7gesendet§8!")));
            } else {
                Optional<Player> targetPlayer = server.getPlayer(choose);
                if (targetPlayer.isPresent()) {
                    connectAndNotify(targetPlayer.get(), targetServer, sender);
                    sender.sendMessage(Messages.prefix(config).append(
                            Messages.legacy("§7Du hast §b" + targetPlayer.get().getUsername() + " §7auf den Server §b" + targetServer.getServerInfo().getName() + " §7gesendet§8!")));
                } else {
                    Optional<RegisteredServer> sourceServer = server.getServer(choose);
                    if (sourceServer.isPresent()) {
                        Collection<Player> playersOnServer = sourceServer.get().getPlayersConnected();
                        for (Player p : playersOnServer) {
                            connectAndNotify(p, targetServer, sender);
                        }
                        sender.sendMessage(Messages.prefix(config).append(
                                Messages.legacy("§7Du hast §balle Spieler von Server §b" + choose + " §7auf den Server §b" + targetServer.getServerInfo().getName() + " §7gesendet§8!")));
                    } else {
                        sender.sendMessage(Messages.prefix(config).append(
                                Messages.legacy("§cDer Spieler/Server §e" + choose + " §7wurde nicht gefunden§8!")));
                    }
                }
            }
            return;
        }

        sendUsage(sender);
    }

    private void connectAndNotify(Player target, RegisteredServer destination, Player sender) {
        target.createConnectionRequest(destination).fireAndForget();
        target.sendMessage(Messages.prefix(config).append(
                Messages.legacy("§7Du wurdest von §b" + sender.getUsername() + " §7zum Server §b" + destination.getServerInfo().getName() + " §7gesendet§8!")));
    }

    private void sendUsage(Player sender) {
        sender.sendMessage(Messages.prefix(config).append(Messages.legacy(" ")));
        sender.sendMessage(Messages.prefix(config).append(Messages.legacy("§b§lSend/Pull §8▪ §7Verwendung")));
        sender.sendMessage(Messages.prefix(config).append(Messages.legacy(" ")));
        sender.sendMessage(Messages.prefix(config).append(Messages.legacy(" §8» §7/send §8(§7Spieler§8) §8(§7Server§8)")));
        sender.sendMessage(Messages.prefix(config).append(Messages.legacy(" §8» §7/send §8(§7Server§8) §8(§7Server§8)")));
        sender.sendMessage(Messages.prefix(config).append(Messages.legacy(" §8» §7/send §8(§7all§8) §8(§7Server§8)")));
        sender.sendMessage(Messages.prefix(config).append(Messages.legacy(" ")));
        sender.sendMessage(Messages.prefix(config).append(Messages.legacy(" §8» §7/pull §8(§7Spieler§8)")));
        sender.sendMessage(Messages.prefix(config).append(Messages.legacy(" §8» §7/pull §8(§7Server§8)")));
        sender.sendMessage(Messages.prefix(config).append(Messages.legacy(" §8» §7/pull §8(§7all§8)")));
        sender.sendMessage(Messages.prefix(config).append(Messages.legacy(" ")));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> suggestions = new java.util.ArrayList<>();
            server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .forEach(suggestions::add);
            server.getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .forEach(suggestions::add);
            if ("all".startsWith(input)) suggestions.add("all");
            return CompletableFuture.completedFuture(suggestions);
        }
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            return CompletableFuture.completedFuture(
                    server.getAllServers().stream()
                            .map(s -> s.getServerInfo().getName())
                            .filter(n -> n.toLowerCase().startsWith(input))
                            .toList());
        }
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("syncproxy.command.send");
    }
}
