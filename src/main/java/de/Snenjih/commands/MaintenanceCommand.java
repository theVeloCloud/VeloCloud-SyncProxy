package de.Snenjih.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.maintenance.MaintenanceManager;
import de.Snenjih.util.Messages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MaintenanceCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;
    private final MaintenanceManager maintenance;

    public MaintenanceCommand(ProxyServer server, ConfigManager config, MaintenanceManager maintenance) {
        this.server = server;
        this.config = config;
        this.maintenance = maintenance;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        if (!source.hasPermission("syncproxy.command.maintenance")) {
            source.sendMessage(Messages.noPermission());
            return;
        }

        if (!config.getBoolean("maintenance.enabled", true)) {
            source.sendMessage(Messages.legacy("§cDas Wartungsmodus-Feature ist deaktiviert."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            sendUsage(invocation);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "on" -> {
                if (maintenance.isActive()) {
                    source.sendMessage(Messages.prefix(config).append(
                            Messages.legacy("§cDer Wartungsmodus ist bereits aktiv.")));
                    return;
                }
                maintenance.setActive(true);
                source.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§aWartungsmodus wurde §laktiviert§r§a.")));
                kickNonWhitelisted();
            }
            case "off" -> {
                if (!maintenance.isActive()) {
                    source.sendMessage(Messages.prefix(config).append(
                            Messages.legacy("§cDer Wartungsmodus ist bereits deaktiviert.")));
                    return;
                }
                maintenance.setActive(false);
                source.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§aWartungsmodus wurde §ldeaktiviert§r§a.")));
            }
            case "status" -> {
                String state = maintenance.isActive() ? "§aaktiv" : "§cinaktiv";
                source.sendMessage(Messages.prefix(config).append(
                        Messages.legacy("§7Wartungsmodus: " + state +
                                " §8| §7Whitelist: §e" + maintenance.getWhitelist().size() + " Einträge")));
            }
            case "add" -> {
                if (args.length < 2) {
                    source.sendMessage(Messages.prefix(config).append(
                            Messages.legacy("§cVerwendung: /maintenance add <Spieler>")));
                    return;
                }
                String name = args[1];
                if (maintenance.addToWhitelist(name)) {
                    source.sendMessage(Messages.prefix(config).append(
                            Messages.legacy("§a" + name + " §7wurde zur Whitelist hinzugefügt.")));
                } else {
                    source.sendMessage(Messages.prefix(config).append(
                            Messages.legacy("§e" + name + " §7ist bereits auf der Whitelist.")));
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    source.sendMessage(Messages.prefix(config).append(
                            Messages.legacy("§cVerwendung: /maintenance remove <Spieler>")));
                    return;
                }
                String name = args[1];
                if (maintenance.removeFromWhitelist(name)) {
                    source.sendMessage(Messages.prefix(config).append(
                            Messages.legacy("§c" + name + " §7wurde von der Whitelist entfernt.")));
                } else {
                    source.sendMessage(Messages.prefix(config).append(
                            Messages.legacy("§e" + name + " §7ist nicht auf der Whitelist.")));
                }
            }
            case "list" -> {
                List<String> wl = maintenance.getWhitelist();
                if (wl.isEmpty()) {
                    source.sendMessage(Messages.prefix(config).append(
                            Messages.legacy("§7Die Whitelist ist leer.")));
                } else {
                    source.sendMessage(Messages.prefix(config).append(
                            Messages.legacy("§7Whitelist §8(" + wl.size() + "): §e" + String.join("§8, §e", wl))));
                }
            }
            default -> sendUsage(invocation);
        }
    }

    private void kickNonWhitelisted() {
        String kickRaw = config.getString("maintenance.kick-message",
                "&c&lWartungsmodus\n&7Der Server befindet sich im Wartungsmodus.\n&7Bitte versuche es später erneut.");
        var kickMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(kickRaw);

        server.getAllPlayers().stream()
                .filter(p -> !p.hasPermission("syncproxy.maintenance.bypass"))
                .filter(p -> !maintenance.isWhitelisted(p.getUsername()))
                .forEach(p -> p.disconnect(kickMessage));
    }

    private void sendUsage(Invocation invocation) {
        var source = invocation.source();
        source.sendMessage(Messages.legacy(
                "§8§m---§r §bWartungsmodus-Befehle §8§m---\n" +
                "§e/maintenance on §8- §7Wartungsmodus aktivieren\n" +
                "§e/maintenance off §8- §7Wartungsmodus deaktivieren\n" +
                "§e/maintenance status §8- §7Status anzeigen\n" +
                "§e/maintenance add <Spieler> §8- §7Zur Whitelist hinzufügen\n" +
                "§e/maintenance remove <Spieler> §8- §7Von der Whitelist entfernen\n" +
                "§e/maintenance list §8- §7Whitelist anzeigen"));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String input = args.length == 1 ? args[0].toLowerCase() : "";
            return CompletableFuture.completedFuture(
                    List.of("on", "off", "status", "add", "remove", "list").stream()
                            .filter(s -> s.startsWith(input))
                            .toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            String input = args[1].toLowerCase();
            if (args[0].equalsIgnoreCase("remove")) {
                return CompletableFuture.completedFuture(
                        maintenance.getWhitelist().stream()
                                .filter(s -> s.toLowerCase().startsWith(input))
                                .toList());
            }
            return CompletableFuture.supplyAsync(() ->
                    server.getAllPlayers().stream()
                            .map(Player::getUsername)
                            .filter(n -> n.toLowerCase().startsWith(input))
                            .toList());
        }
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("syncproxy.command.maintenance");
    }
}
