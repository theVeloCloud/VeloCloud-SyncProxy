package de.Snenjih.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.maintenance.MaintenanceManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MotdListener {

    private final ProxyServer server;
    private final ConfigManager config;
    private final MaintenanceManager maintenance;

    public MotdListener(ProxyServer server, ConfigManager config, MaintenanceManager maintenance) {
        this.server = server;
        this.config = config;
        this.maintenance = maintenance;
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        if (!config.getBoolean("motd.enabled", true)) return;

        String prefix = maintenance.isActive() ? "maintenance.motd" : "motd";
        String line1 = config.getString(prefix + ".line1", "&7A Minecraft Network");
        String line2 = config.getString(prefix + ".line2", "");

        String raw = line2.isBlank() ? line1 : (line1 + "\n" + line2);
        Component description = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);

        ServerPing ping = event.getPing();
        ServerPing.Builder builder = ping.asBuilder();
        builder.description(description);

        int maxPlayers = config.getInt(prefix + ".max-players", -1);
        if (maxPlayers >= 0) {
            builder.maximumPlayers(maxPlayers);
        }

        int offset = config.getInt("motd.online-players-offset", 0);
        if (offset != 0) {
            builder.onlinePlayers(server.getAllPlayers().size() + offset);
        }

        event.setPing(builder.build());
    }
}
