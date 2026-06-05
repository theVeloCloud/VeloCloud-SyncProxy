package de.Snenjih.maintenance;

import de.Snenjih.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceManager {

    private final ConfigManager config;
    private boolean active;
    private final List<String> whitelist = new ArrayList<>();

    public MaintenanceManager(ConfigManager config) {
        this.config = config;
        this.active = config.getBoolean("maintenance.active", false);
        reloadWhitelist();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        config.setNested("maintenance.active", active);
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public boolean isWhitelisted(String username) {
        for (String entry : whitelist) {
            if (entry.equalsIgnoreCase(username)) return true;
        }
        return false;
    }

    public boolean addToWhitelist(String username) {
        if (isWhitelisted(username)) return false;
        whitelist.add(username);
        config.setNested("maintenance.whitelist", new ArrayList<>(whitelist));
        return true;
    }

    public boolean removeFromWhitelist(String username) {
        boolean removed = whitelist.removeIf(e -> e.equalsIgnoreCase(username));
        if (removed) config.setNested("maintenance.whitelist", new ArrayList<>(whitelist));
        return removed;
    }

    public void reloadWhitelist() {
        whitelist.clear();
        whitelist.addAll(config.getStringList("maintenance.whitelist", List.of()));
    }
}
