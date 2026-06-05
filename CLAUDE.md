# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

VeloCloud-SyncProxy is a Velocity proxy plugin written in Java 21. It wraps the VeloCloud cloud system (`de.snenjih.velocloud:bridge-velocity`) and exposes proxy-level features: ban/punishment system, maintenance mode, private messaging, tablist, join routing, reconnect handling, MOTD, and service notifications.

## Build & Development Commands

```bash
./gradlew build           # Full build
./gradlew clean build     # Clean build from scratch
./gradlew runVelocity     # Run Velocity server with plugin loaded for testing
```

- **Java Version**: 21 (toolchain in `build.gradle.kts`)
- **Repositories**: Maven Central, PaperMC (`repo.papermc.io`), and the private `repo.snenjih.de/releases` (hosts the VeloCloud SDK)
- **Template Expansion**: `BuildConstants.java` is generated from `src/main/templates/` — Gradle replaces `${version}` at compile time. Run `generateTemplates` task manually or let IDE sync trigger it.

## Architecture

### Initialization (`SyncProxy.java`)

All wiring happens in `onProxyInitialization`. The method:
1. Instantiates `ConfigManager` and `MaintenanceManager`
2. Tries to get a `DataSource` from `Velocloud.instance().databaseProvider().cloudDataSource()` — the ban system is disabled entirely if this returns null
3. Registers listeners and commands with Velocity's event/command managers
4. Subscribes to `ServiceChangeStateEvent` via VeloCloud's own event provider (not Velocity's)

### Key Subsystems

**ConfigManager** (`config/`): Reads/writes `config.yml` using SnakeYAML. Access values via dot-notation keys (`config.getString("ban.reason", default)`). `setNested()` persists changes back to disk immediately.

**BanManager** (`ban/`): MySQL-backed punishment system using a `DataSource` from VeloCloud. Table: `sp_punishments`. Uses an in-memory `BanCache` for fast login checks, refreshed on a configurable interval. All DB operations are async via `CompletableFuture.supplyAsync`. Supports offline bans (UUID/IP backfilled on next login via `backfillPlayerData`).

**MaintenanceManager** (`maintenance/`): Thin wrapper over `ConfigManager`. State (`active`) is persisted to `config.yml` on change.

**LobbyRouter** (`util/`): Picks the least-full `RegisteredServer` in a named VeloCloud service group. Used by join routing, reconnect, and `/hub`.

**Messages** (`util/`): Helper for building Adventure `Component`s from legacy `§`-codes or config values.

**PrivateMessageManager** (`messaging/`): Holds in-memory last-sender map for `/reply`.

**TablistTask** (`tablist/`): Implements `Runnable`, scheduled by Velocity's scheduler at a configurable interval. Replaces placeholders (`%online_players%`, `%server%`, `%ping%`, `%time%`, etc.) in header/footer lines from config.

### Listener Overview

| Listener | Velocity Event | Purpose |
|---|---|---|
| `JoinLeaveListener` | `PostLoginEvent`, `DisconnectEvent` | Broadcasts join/leave messages; clears PM reply state |
| `JoinRoutingListener` | `PlayerChooseInitialServerEvent` | Routes new joins to least-full lobby |
| `ReconnectListener` | `KickedFromServerEvent` | Redirects to fallback on server crash |
| `LoginListener` | `PreLoginEvent` | Blocks banned players |
| `MotdListener` | `ProxyPingEvent` | Sets MOTD and player count |
| `MaintenanceListener` | `LoginEvent` | Kicks non-whitelisted players during maintenance |
| `TablistListener` | `PostLoginEvent` | Applies tablist to joining players |
| `AltDetectionListener` | `PostLoginEvent` | Warns staff if player's IP matches a banned account |
| `ServiceEventListener` | VeloCloud `ServiceChangeStateEvent` | Notifies staff of service start/stop |

### Permission Nodes

| Permission | Purpose |
|---|---|
| `syncproxy.notify.service` | Receive service start/stop notifications |
| `syncproxy.notify.altdetection` | Receive alt-account alerts |
| `syncproxy.hide.joinleave` | Hide own join/leave message |
| `syncproxy.maintenance.bypass` | Join during maintenance |

### Adding a New Feature

1. Create your manager/listener class under the appropriate package
2. Register it in `SyncProxy.onProxyInitialization`
3. Add config keys to `src/main/resources/config.yml` with comments
4. Access config via `configManager.getString/getBoolean/getInt/getStringList`
5. Use `Messages.legacy()` or `Messages.prefix()` for player-facing text

### VeloCloud SDK Integration

The plugin depends on `de.snenjih.velocloud:bridge-velocity`. Key entry point: `Velocloud.instance()`. Used for:
- `databaseProvider().cloudDataSource()` — shared MySQL connection pool
- `serviceProvider().findByGroup(groupName)` — list services in a group
- `eventProvider().subscribe(...)` — subscribe to cloud-level events (separate from Velocity events)
