package de.Snenjih.util;

import de.Snenjih.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Messages {

    private Messages() {}

    public static Component prefix(ConfigManager config) {
        return legacy(config.getString("prefix", "§8[§bSyncProxy§8] "));
    }

    public static Component noPermission() {
        return legacy("§cDu hast keine Berechtigung für diesen Command§8!");
    }

    public static Component legacy(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }
}
