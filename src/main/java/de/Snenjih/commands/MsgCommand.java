package de.Snenjih.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.messaging.PrivateMessageManager;
import de.Snenjih.util.Messages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MsgCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;
    private final PrivateMessageManager pmm;

    public MsgCommand(ProxyServer server, ConfigManager config, PrivateMessageManager pmm) {
        this.server = server;
        this.config = config;
        this.pmm = pmm;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 2) {
            source.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§7Verwendung: §b/msg <Spieler> <Nachricht>")));
            return;
        }

        Optional<Player> targetOpt = server.getPlayer(args[0]);
        if (targetOpt.isEmpty()) {
            source.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§c" + args[0] + " §7ist nicht online.")));
            return;
        }

        Player target = targetOpt.get();
        UUID senderUuid = source instanceof Player p ? p.getUniqueId() : null;

        if (senderUuid != null && senderUuid.equals(target.getUniqueId())) {
            source.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§cDu kannst dir keine Nachricht an dich selbst schicken.")));
            return;
        }

        String senderName = source instanceof Player p ? p.getUsername() : "Konsole";
        String messageText = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        String toFormat = config.getString("private-message.format-to",
                "&8[&7Ich &8→ &b%target%&8] &7%message%");
        String fromFormat = config.getString("private-message.format-from",
                "&8[&b%sender% &8→ &7Ich&8] &7%message%");

        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                toFormat.replace("%target%", target.getUsername())
                        .replace("%sender%", senderName)
                        .replace("%message%", messageText)));

        target.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                fromFormat.replace("%sender%", senderName)
                          .replace("%target%", target.getUsername())
                          .replace("%message%", messageText)));

        // Update reply targets so both sides can /reply
        if (senderUuid != null) {
            pmm.setLastMessaged(senderUuid, target.getUniqueId());
            pmm.setLastMessaged(target.getUniqueId(), senderUuid);
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
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
