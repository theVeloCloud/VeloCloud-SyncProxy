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
import de.Snenjih.listener.*;
import de.Snenjih.maintenance.MaintenanceManager;
import de.Snenjih.messaging.PrivateMessageManager;
import de.Snenjih.tablist.TablistTask;
import de.snenjih.velocloud.sdk.java.Velocloud;
import de.snenjih.velocloud.shared.events.definitions.service.ServiceChangeStateEvent;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

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
    private MaintenanceManager maintenanceManager;
    private Instant startTime;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        startTime = Instant.now();
        configManager = new ConfigManager(dataDirectory, logger);
        maintenanceManager = new MaintenanceManager(configManager);

        // Feature: Join/Leave-Nachrichten + Reply-Cleanup
        PrivateMessageManager pmm = new PrivateMessageManager();
        server.getEventManager().register(this, new JoinLeaveListener(server, configManager, pmm));

        // Feature: Join-Routing
        server.getEventManager().register(this, new JoinRoutingListener(server, configManager));

        // Feature: Reconnect-Handler
        server.getEventManager().register(this, new ReconnectListener(server, configManager));

        DataSource dataSource = Velocloud.instance().databaseProvider().cloudDataSource();
        if (dataSource == null) {
            logger.error("[SyncProxy] Keine Datenbankverbindung verfügbar! Ban-System deaktiviert.");
        } else {
            banManager = new BanManager(server, this, configManager, dataSource, logger);
            banManager.init();
            server.getEventManager().register(this, new LoginListener(server, banManager, configManager));
        }

        // Feature: MOTD
        server.getEventManager().register(this, new MotdListener(server, configManager, maintenanceManager));

        // Feature: Wartungsmodus
        server.getEventManager().register(this, new MaintenanceListener(configManager, maintenanceManager));

        // Feature: Tablist
        TablistTask tablistTask = new TablistTask(server, configManager);
        server.getEventManager().register(this, new TablistListener(configManager, tablistTask));
        if (configManager.getBoolean("tablist.enabled", true)) {
            int interval = configManager.getInt("tablist.update-interval", 2);
            server.getScheduler().buildTask(this, tablistTask)
                    .repeat(interval, TimeUnit.SECONDS)
                    .schedule();
        }

        // Feature: Service-Notifications
        ServiceEventListener serviceEventListener = new ServiceEventListener(server, configManager);
        Velocloud.instance().eventProvider()
                .subscribe(ServiceChangeStateEvent.class, serviceEventListener::onServiceStateChange);

        var cm = server.getCommandManager();
        cm.register(cm.metaBuilder("cstart").aliases("servicestart").build(),
                new CstartCommand(server, configManager));
        cm.register(cm.metaBuilder("cstop").aliases("servicestop").build(),
                new CstopCommand(server, configManager));
        cm.register(cm.metaBuilder("crestart").aliases("cloudrestart", "restart").build(),
                new CrestartCommand(server, configManager));
        cm.register(cm.metaBuilder("server").build(),
                new ServerCommand(server, configManager));
        cm.register(cm.metaBuilder("send").aliases("pull").build(),
                new SendCommand(server, configManager));
        cm.register(cm.metaBuilder("catch").build(),
                new CatchCommand(server, configManager));
        cm.register(cm.metaBuilder("ping").build(),
                new PingCommand(server, configManager));
        cm.register(cm.metaBuilder("broadcast").aliases("bc").build(),
                new BroadcastCommand(server, configManager));
        cm.register(cm.metaBuilder("maintenance").aliases("maint").build(),
                new MaintenanceCommand(server, configManager, maintenanceManager));
        cm.register(cm.metaBuilder("msg").aliases("tell", "whisper", "w").build(),
                new MsgCommand(server, configManager, pmm));
        cm.register(cm.metaBuilder("reply").aliases("r").build(),
                new ReplyCommand(server, configManager, pmm));
        cm.register(cm.metaBuilder("network").build(),
                new NetworkCommand(server, configManager));
        cm.register(cm.metaBuilder("uptime").build(),
                new UptimeCommand(configManager, startTime));
        cm.register(cm.metaBuilder("hub").aliases("lobby").build(),
                new HubCommand(server, configManager));

        if (banManager != null) {
            cm.register(cm.metaBuilder("punish").build(),
                    new PunishCommand(server, configManager, banManager));
            cm.register(cm.metaBuilder("pardon").build(),
                    new PardonCommand(server, configManager, banManager));
            cm.register(cm.metaBuilder("check").build(),
                    new CheckCommand(server, configManager, banManager));
            cm.register(cm.metaBuilder("kick").build(),
                    new KickCommand(server, configManager, banManager));
            cm.register(cm.metaBuilder("info").build(),
                    new InfoCommand(server, configManager, banManager));
            server.getEventManager().register(this, new AltDetectionListener(server, banManager, configManager, logger));
            logger.info("SyncProxy loaded. 20 commands registered (inkl. Ban-System).");
        } else {
            logger.info("SyncProxy loaded. 15 commands registered (Ban-System deaktiviert).");
        }
    }
}
