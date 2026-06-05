package de.Snenjih.tablist;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TablistTask implements Runnable {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ProxyServer server;
    private final ConfigManager config;

    public TablistTask(ProxyServer server, ConfigManager config) {
        this.server = server;
        this.config = config;
    }

    @Override
    public void run() {
        if (!config.getBoolean("tablist.enabled", true)) return;

        List<String> headerLines = config.getStringList("tablist.header", List.of());
        List<String> footerLines = config.getStringList("tablist.footer", List.of());

        int onlinePlayers = server.getAllPlayers().size();
        int maxPlayers = config.getInt("motd.max-players", server.getConfiguration().getShowMaxPlayers());
        String time = LocalTime.now().format(TIME_FMT);

        for (Player player : server.getAllPlayers()) {
            String serverName = player.getCurrentServer()
                    .map(s -> s.getServerInfo().getName())
                    .orElse("?");
            long ping = player.getPing();

            Component header = buildComponent(headerLines, player, serverName, ping, onlinePlayers, maxPlayers, time);
            Component footer = buildComponent(footerLines, player, serverName, ping, onlinePlayers, maxPlayers, time);
            player.sendPlayerListHeaderAndFooter(header, footer);
        }
    }

    private Component buildComponent(List<String> lines, Player player, String serverName,
                                     long ping, int onlinePlayers, int maxPlayers, String time) {
        if (lines.isEmpty()) return Component.empty();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(resolve(lines.get(i), player, serverName, ping, onlinePlayers, maxPlayers, time));
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(sb.toString());
    }

    private String resolve(String text, Player player, String serverName,
                            long ping, int onlinePlayers, int maxPlayers, String time) {
        return text
                .replace("%name%", player.getUsername())
                .replace("%server%", serverName)
                .replace("%ping%", String.valueOf(ping))
                .replace("%online_players%", String.valueOf(onlinePlayers))
                .replace("%max_players%", String.valueOf(maxPlayers))
                .replace("%time%", time)
                .replace("%proxy%", config.getString("tablist.proxy-name", "Proxy"))
                .replace("%proxy_uniqueId%", config.getString("tablist.proxy-uuid", ""))
                .replace("%proxy_group_name%", config.getString("tablist.proxy-group", ""));
    }

    public void updatePlayer(Player player) {
        if (!config.getBoolean("tablist.enabled", true)) return;

        List<String> headerLines = config.getStringList("tablist.header", List.of());
        List<String> footerLines = config.getStringList("tablist.footer", List.of());

        int onlinePlayers = server.getAllPlayers().size();
        int maxPlayers = config.getInt("motd.max-players", server.getConfiguration().getShowMaxPlayers());
        String time = LocalTime.now().format(TIME_FMT);
        String serverName = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("?");
        long ping = player.getPing();

        Component header = buildComponent(headerLines, player, serverName, ping, onlinePlayers, maxPlayers, time);
        Component footer = buildComponent(footerLines, player, serverName, ping, onlinePlayers, maxPlayers, time);
        player.sendPlayerListHeaderAndFooter(header, footer);
    }
}
