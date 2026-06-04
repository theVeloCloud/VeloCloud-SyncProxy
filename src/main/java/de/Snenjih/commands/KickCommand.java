package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.ban.BanManager;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.Messages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class KickCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;
    private final BanManager banManager;

    public KickCommand(ProxyServer server, ConfigManager config, BanManager banManager) {
        this.server = server;
        this.config = config;
        this.banManager = banManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("syncproxy.command.kick")) {
            source.sendMessage(Messages.noPermission());
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 1) {
            source.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§7Nutzung: §b/kick §8(§7Spieler§8) §8[§7Grund§8]")));
            return;
        }

        String target = args[0];
        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "Kein Grund angegeben";

        Optional<Player> optPlayer = server.getPlayer(target);
        if (optPlayer.isEmpty()) {
            source.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§c" + target + " §7ist nicht online.")));
            return;
        }

        Player player = optPlayer.get();
        String bannerName = source instanceof Player p ? p.getUsername() : "CONSOLE";
        String bannerUuid = source instanceof Player p ? p.getUniqueId().toString() : null;

        server.getScheduler().buildTask(this, () -> {
            banManager.logKick(target, bannerName, bannerUuid, reason).join();

            String kickMsg = config.getString("kick-message",
                    "§8§m----------§r §e§lGekickt §8§m----------\n§7Grund: §e{reason}\n§7Gekickt von: §e{banner}");
            kickMsg = kickMsg.replace("{reason}", reason).replace("{banner}", bannerName);

            player.disconnect(LegacyComponentSerializer.legacySection().deserialize(kickMsg));
            source.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§7Spieler §e" + target + " §7wurde gekickt. §8(§7Grund: §e" + reason + "§8)")));
        }).schedule();
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return CompletableFuture.completedFuture(
                    server.getAllPlayers().stream()
                            .map(p -> p.getUsername())
                            .filter(n -> n.toLowerCase().startsWith(input))
                            .toList());
        }
        return CompletableFuture.completedFuture(List.of());
    }
}
