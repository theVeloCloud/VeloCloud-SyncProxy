package de.Snenjih.listener;

import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.maintenance.MaintenanceManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MaintenanceListener {

    private final ConfigManager config;
    private final MaintenanceManager maintenance;

    public MaintenanceListener(ConfigManager config, MaintenanceManager maintenance) {
        this.config = config;
        this.maintenance = maintenance;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        if (!config.getBoolean("maintenance.enabled", true)) return;
        if (!maintenance.isActive()) return;

        var player = event.getPlayer();
        if (player.hasPermission("syncproxy.maintenance.bypass")) return;
        if (maintenance.isWhitelisted(player.getUsername())) return;

        String kickRaw = config.getString("maintenance.kick-message",
                "&c&lWartungsmodus\n&7Der Server befindet sich im Wartungsmodus.\n&7Bitte versuche es später erneut.");
        var kickMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(kickRaw);
        event.setResult(ComponentResult.denied(kickMessage));
    }
}
