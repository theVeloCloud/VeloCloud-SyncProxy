package de.Snenjih.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.LobbyRouter;
import de.Snenjih.util.Messages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ReconnectListener {

    private final ProxyServer server;
    private final ConfigManager config;

    public ReconnectListener(ProxyServer server, ConfigManager config) {
        this.server = server;
        this.config = config;
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        if (!config.getBoolean("reconnect.enabled", true)) return;

        // Only redirect when the server disconnected the player without a reason (crash/unexpected stop).
        // Explicit server-side kicks always supply a reason component.
        if (event.getServerKickReason().isPresent()) return;

        String groupName = config.getString("reconnect.fallback-group", "Lobby");
        RegisteredServer lobby = LobbyRouter.pickLeastFull(server, groupName);

        if (lobby == null) {
            // Try the static fallback server from config
            String staticFallback = config.getString("fallback-server", "Lobby-1");
            lobby = server.getServer(staticFallback).orElse(null);
        }

        if (lobby == null) {
            String noFallback = config.getString("reconnect.no-fallback-message",
                    "&cKein Fallback-Server verfügbar. Bitte versuche es erneut.");
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(
                    LegacyComponentSerializer.legacyAmpersand().deserialize(noFallback)));
            return;
        }

        String redirectMsg = config.getString("reconnect.redirect-message",
                "&7Der Server wurde gestoppt. Du wirst zur Lobby weitergeleitet.");
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(
                lobby,
                LegacyComponentSerializer.legacyAmpersand().deserialize(redirectMsg)));
    }
}
