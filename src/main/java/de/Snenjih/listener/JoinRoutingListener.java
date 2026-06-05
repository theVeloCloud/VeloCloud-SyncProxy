package de.Snenjih.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.LobbyRouter;

public class JoinRoutingListener {

    private final ProxyServer server;
    private final ConfigManager config;

    public JoinRoutingListener(ProxyServer server, ConfigManager config) {
        this.server = server;
        this.config = config;
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        if (!config.getBoolean("join-routing.enabled", true)) return;

        // Only route the initial connection (player has no current server yet)
        if (event.getPlayer().getCurrentServer().isPresent()) return;

        String groupName = config.getString("join-routing.fallback-group", "Lobby");
        RegisteredServer target = LobbyRouter.pickLeastFull(server, groupName);
        if (target == null) return;

        event.setResult(ServerPreConnectEvent.ServerResult.allowed(target));
    }
}
