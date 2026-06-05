package de.Snenjih.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.messaging.PrivateMessageManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

public class JoinLeaveListener {

    private final ProxyServer server;
    private final ConfigManager config;
    private final PrivateMessageManager pmm;

    public JoinLeaveListener(ProxyServer server, ConfigManager config, PrivateMessageManager pmm) {
        this.server = server;
        this.config = config;
        this.pmm = pmm;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (!config.getBoolean("join-leave.enabled", true)) return;

        Player joining = event.getPlayer();
        if (joining.hasPermission("syncproxy.hide.joinleave")) return;

        String template = config.getString("join-leave.join-message",
                "&8[&a+&8] &7%player% &7hat das Netzwerk betreten.");
        var component = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(template.replace("%player%", joining.getUsername()));

        UUID joiningUuid = joining.getUniqueId();
        server.getAllPlayers().stream()
                .filter(p -> !p.getUniqueId().equals(joiningUuid))
                .forEach(p -> p.sendMessage(component));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player leaving = event.getPlayer();

        // Clean up private message reply state
        pmm.cleanup(leaving.getUniqueId());

        if (!config.getBoolean("join-leave.enabled", true)) return;
        if (leaving.hasPermission("syncproxy.hide.joinleave")) return;

        String template = config.getString("join-leave.leave-message",
                "&8[&c-&8] &7%player% &7hat das Netzwerk verlassen.");
        var component = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(template.replace("%player%", leaving.getUsername()));

        server.getAllPlayers().forEach(p -> p.sendMessage(component));
    }
}
