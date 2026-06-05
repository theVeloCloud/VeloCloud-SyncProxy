package de.Snenjih.util;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.snenjih.velocloud.sdk.java.Velocloud;
import de.snenjih.velocloud.shared.service.Service;
import de.snenjih.velocloud.v1.services.ServiceState;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class LobbyRouter {

    private LobbyRouter() {}

    /**
     * Returns the RegisteredServer with the fewest players in the given VeloCloud service group,
     * considering only ONLINE services. Returns null if no suitable server is found.
     */
    public static RegisteredServer pickLeastFull(ProxyServer proxy, String groupName) {
        try {
            List<Service> services = Velocloud.instance().serviceProvider().findByGroup(groupName);
            return services.stream()
                    .filter(s -> s.getState() == ServiceState.ONLINE)
                    .map(s -> proxy.getServer(s.name()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .min(Comparator.comparingInt(rs -> rs.getPlayersConnected().size()))
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
