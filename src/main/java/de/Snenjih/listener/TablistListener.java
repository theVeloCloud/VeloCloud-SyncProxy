package de.Snenjih.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.tablist.TablistTask;

public class TablistListener {

    private final ConfigManager config;
    private final TablistTask tablistTask;

    public TablistListener(ConfigManager config, TablistTask tablistTask) {
        this.config = config;
        this.tablistTask = tablistTask;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (!config.getBoolean("tablist.enabled", true)) return;
        tablistTask.updatePlayer(event.getPlayer());
    }
}
