package de.Snenjih.commands;

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

public class ReplyCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager config;
    private final PrivateMessageManager pmm;

    public ReplyCommand(ProxyServer server, ConfigManager config, PrivateMessageManager pmm) {
        this.server = server;
        this.config = config;
        this.pmm = pmm;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player sender)) {
            invocation.source().sendMessage(Messages.legacy("§cNur Spieler können /reply verwenden."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 1) {
            sender.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§7Verwendung: §b/reply <Nachricht>")));
            return;
        }

        Optional<UUID> lastUuid = pmm.getLastMessaged(sender.getUniqueId());
        if (lastUuid.isEmpty()) {
            sender.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§cDu hast niemanden, dem du antworten kannst.")));
            return;
        }

        Optional<Player> targetOpt = server.getPlayer(lastUuid.get());
        if (targetOpt.isEmpty()) {
            sender.sendMessage(Messages.prefix(config).append(
                    Messages.legacy("§cDer Spieler ist nicht mehr online.")));
            return;
        }

        Player target = targetOpt.get();
        String messageText = String.join(" ", args);

        String toFormat = config.getString("private-message.format-to",
                "&8[&7Ich &8→ &b%target%&8] &7%message%");
        String fromFormat = config.getString("private-message.format-from",
                "&8[&b%sender% &8→ &7Ich&8] &7%message%");

        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                toFormat.replace("%target%", target.getUsername())
                        .replace("%sender%", sender.getUsername())
                        .replace("%message%", messageText)));

        target.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                fromFormat.replace("%sender%", sender.getUsername())
                          .replace("%target%", target.getUsername())
                          .replace("%message%", messageText)));

        pmm.setLastMessaged(sender.getUniqueId(), target.getUniqueId());
        pmm.setLastMessaged(target.getUniqueId(), sender.getUniqueId());
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(List.of());
    }
}
