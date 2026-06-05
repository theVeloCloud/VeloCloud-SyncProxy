package de.Snenjih.listener;

import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.config.ConfigManager;
import de.snenjih.velocloud.shared.events.definitions.service.ServiceChangeStateEvent;
import de.snenjih.velocloud.shared.service.Service;
import de.snenjih.velocloud.v1.services.ServiceState;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ServiceEventListener {

    private final ProxyServer server;
    private final ConfigManager config;

    public ServiceEventListener(ProxyServer server, ConfigManager config) {
        this.server = server;
        this.config = config;
    }

    public void onServiceStateChange(ServiceChangeStateEvent event) {
        if (!config.getBoolean("service-notifications.enabled", true)) return;

        Service service = event.getService();
        ServiceState state = service.getState();

        String template;
        if (state == ServiceState.ONLINE) {
            template = config.getString("service-notifications.start-message",
                    "&8[&aSERVICE&8] &7Service &a%service% &7(&e%group%&7) ist &aonline&7.");
        } else if (state == ServiceState.STOPPING) {
            template = config.getString("service-notifications.stop-message",
                    "&8[&cSERVICE&8] &7Service &c%service% &7(&e%group%&7) wurde &cgestoppt&7.");
        } else {
            return;
        }

        String message = template
                .replace("%service%", service.name())
                .replace("%group%", service.getGroupName())
                .replace("%state%", state.name());

        var component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        String permission = config.getString("service-notifications.permission", "syncproxy.notify.service");

        server.getAllPlayers().stream()
                .filter(p -> p.hasPermission(permission))
                .forEach(p -> p.sendMessage(component));
    }
}
