# VeloCloud-SyncProxy

A [Velocity](https://papermc.io/software/velocity) proxy plugin for the VeloCloud network infrastructure. Provides proxy-level management features built on top of the VeloCloud SDK.

## Features

- **Ban/Punishment System** — MySQL-backed bans and kicks with offline-ban support, IP cascade banning, and alt-account detection
- **Maintenance Mode** — Toggle maintenance with per-player whitelist; persists across restarts
- **Join Routing** — Routes new players to the least-full lobby server
- **Reconnect Handler** — Redirects players to a fallback server on backend crash
- **MOTD** — Configurable server list description and player count
- **Tablist** — Dynamic header/footer with player/server/ping placeholders, updated on an interval
- **Private Messaging** — `/msg`, `/reply` with configurable format
- **Service Notifications** — Staff alerts on VeloCloud service start/stop events
- **Utility Commands** — `/hub`, `/network`, `/uptime`, `/ping`, `/broadcast`, `/send`, `/catch`, `/server`, `/cstart`, `/cstop`, `/crestart`

## Requirements

- Java 21+
- Velocity 3.5.0+
- VeloCloud with `bridge-velocity` (provides the shared MySQL data source and service/event APIs)

## Installation

1. Build the plugin: `./gradlew build`
2. Copy the JAR from `build/libs/` to your Velocity `plugins/` folder
3. Restart Velocity — `config.yml` is generated on first run in `plugins/syncproxy/`

## Configuration

All features are configured in `plugins/syncproxy/config.yml`. Each section can be disabled individually with `enabled: false`.

Key sections:

| Section | Description |
|---|---|
| `ban` | Cache interval, ban-screen message template, IP cascade settings |
| `motd` | Server list lines, max-players override |
| `maintenance` | Active state, kick message, player whitelist |
| `tablist` | Header/footer lines, update interval, placeholder proxy info |
| `join-leave` | Join/leave message formats |
| `private-message` | `/msg` sender/receiver format strings |
| `join-routing` | Lobby group name for initial server routing |
| `reconnect` | Fallback group name and redirect message |
| `hub` | Lobby group name for `/hub` and `/lobby` |
| `service-notifications` | Staff permission, start/stop message templates |
| `alt-detection` | Staff permission, alert message template |

### Placeholders

- **Tablist**: `%name%`, `%server%`, `%ping%`, `%online_players%`, `%max_players%`, `%time%`, `%proxy%`, `%proxy_uniqueId%`, `%proxy_group_name%`
- **Private messages**: `%sender%`, `%target%`, `%message%`
- **Service notifications**: `%service%`, `%group%`, `%state%`
- **Ban screen**: `{player}`, `{banner}`, `{reason}`, `{banned_at}`, `{expires_at}`, `{time_remaining}`
- **Join/leave**: `%player%`
- **Alt detection**: `%player%`, `%accounts%`

## Permissions

| Permission | Description |
|---|---|
| `syncproxy.notify.service` | Receive service start/stop notifications |
| `syncproxy.notify.altdetection` | Receive alt-account alerts |
| `syncproxy.hide.joinleave` | Hide own join/leave message |
| `syncproxy.maintenance.bypass` | Join during maintenance mode |

## Commands

| Command | Aliases | Description |
|---|---|---|
| `/punish <player> <duration> <reason>` | | Ban a player |
| `/pardon <player>` | | Unban a player |
| `/kick <player> <reason>` | | Kick a player |
| `/check <player>` | | Check active ban |
| `/info <player>` | | Show punishment history |
| `/maintenance <on\|off\|add\|remove\|list>` | `/maint` | Manage maintenance mode |
| `/broadcast <message>` | `/bc` | Send network-wide message |
| `/msg <player> <message>` | `/tell`, `/whisper`, `/w` | Send private message |
| `/reply <message>` | `/r` | Reply to last private message |
| `/hub` | `/lobby` | Send yourself to lobby |
| `/send <player\|all> <server>` | `/pull` | Send player(s) to a server |
| `/catch <player>` | | Pull a player to your server |
| `/server <name>` | | Connect to a server |
| `/network` | | Show network overview |
| `/uptime` | | Show proxy uptime |
| `/ping [player]` | | Show ping |
| `/cstart <service>` | `/servicestart` | Start a VeloCloud service |
| `/cstop <service>` | `/servicestop` | Stop a VeloCloud service |
| `/crestart <service>` | `/cloudrestart`, `/restart` | Restart a VeloCloud service |

## License

[MIT](LICENSE)
