package de.Snenjih.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.ban.BanManager;
import de.Snenjih.config.ConfigManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.util.List;

public class AltDetectionListener {

    private final ProxyServer server;
    private final BanManager banManager;
    private final ConfigManager config;
    private final Logger logger;

    public AltDetectionListener(ProxyServer server, BanManager banManager, ConfigManager config, Logger logger) {
        this.server = server;
        this.banManager = banManager;
        this.config = config;
        this.logger = logger;
    }

    @Subscribe(async = true)
    public void onPostLogin(PostLoginEvent event) {
        if (!config.getBoolean("alt-detection.enabled", true)) return;

        Player player = event.getPlayer();
        String ip = player.getRemoteAddress().getAddress().getHostAddress();
        if (ip == null || ip.isBlank()) return;

        List<String> bannedAccounts = banManager.getBannedAccountsByIp(ip).join();
        if (bannedAccounts.isEmpty()) return;

        // Filter out the joining player themselves (they passed the ban check in LoginListener)
        bannedAccounts = bannedAccounts.stream()
                .filter(name -> !name.equalsIgnoreCase(player.getUsername()))
                .toList();
        if (bannedAccounts.isEmpty()) return;

        String accountList = String.join(", ", bannedAccounts);
        String template = config.getString("alt-detection.message",
                "&8[&cALT&8] &7Spieler &c%player% &7verbindet sich mit der IP eines gebannten Accounts: &c%accounts%");
        var component = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(template
                        .replace("%player%", player.getUsername())
                        .replace("%accounts%", accountList));

        String permission = config.getString("alt-detection.notify-permission", "syncproxy.notify.altdetection");
        server.getAllPlayers().stream()
                .filter(p -> p.hasPermission(permission))
                .forEach(p -> p.sendMessage(component));

        logger.warn("[AltDetection] {} verbindet sich von IP {}, bekannte gebannte Accounts: {}",
                player.getUsername(), ip, accountList);
    }
}
