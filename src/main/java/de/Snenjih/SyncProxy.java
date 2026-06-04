package de.Snenjih;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.Snenjih.ban.BanManager;
import de.Snenjih.commands.*;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.listener.LoginListener;
import de.snenjih.velocloud.sdk.java.Velocloud;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.nio.file.Path;

@Plugin(id = "syncproxy", name = "SyncProxy", version = BuildConstants.VERSION, description = "SyncProxy", url = "snenjih.de", authors = {"Snenjih"})
public class SyncProxy {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;

    @Inject
    @DataDirectory
    private Path dataDirectory;

    private ConfigManager configManager;
    private BanManager banManager;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        configManager = new ConfigManager(dataDirectory, logger);

        DataSource dataSource = Velocloud.instance().databaseProvider().cloudDataSource();
        if (dataSource == null) {
            logger.error("[SyncProxy] Keine Datenbankverbindung verfügbar! Ban-System deaktiviert.");
        } else {
            banManager = new BanManager(server, this, configManager, dataSource, logger);
            banManager.init();
            server.getEventManager().register(this, new LoginListener(server, banManager, configManager));
        }

        var cm = server.getCommandManager();
        cm.register(cm.metaBuilder("cstart").aliases("servicestart").build(),
                new CstartCommand(server, configManager));
        cm.register(cm.metaBuilder("cstop").aliases("servicestop").build(),
                new CstopCommand(server, configManager));
        cm.register(cm.metaBuilder("crestart").aliases("cloudrestart", "restart").build(),
                new CrestartCommand(server, configManager));
        cm.register(cm.metaBuilder("server").build(),
                new ServerCommand(server, configManager));

        if (banManager != null) {
            cm.register(cm.metaBuilder("punish").build(),
                    new PunishCommand(server, configManager, banManager));
            cm.register(cm.metaBuilder("pardon").build(),
                    new PardonCommand(server, configManager, banManager));
            cm.register(cm.metaBuilder("check").build(),
                    new CheckCommand(server, configManager, banManager));
            cm.register(cm.metaBuilder("kick").build(),
                    new KickCommand(server, configManager, banManager));
            logger.info("SyncProxy loaded. 8 commands registered (inkl. Ban-System).");
        } else {
            logger.info("SyncProxy loaded. 4 commands registered (Ban-System deaktiviert).");
        }
    }
}
