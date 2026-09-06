# Velolist

A high-performance, security-hardened UUID-based whitelist plugin for the Velocity proxy with native Bedrock (Floodgate) support and in-memory caching.

Forked from [PandaDEV's Vitelist](https://github.com/0PandaDEV/Vitelist) to add Floodgate integration, eliminate proxy Netty thread starvation, fix fail-open authentication bypasses, and optimize connection performance.

## Fork Details & Architectural Enhancements

Velolist is an enhanced, security-hardened fork of **Vitelist** designed for high-traffic networks and crossplay environments:

- **Fail-Closed Authentication Protection:** Original Vitelist caught configuration read errors and logged them without disconnecting the player, creating a critical fail-open bypass. Velolist intercepts authentication at the early `LoginEvent` lifecycle stage and fails closed, safely terminating unauthorized sessions.
- **Zero Netty Thread Starvation (In-Memory Caching):** Original Vitelist performed synchronous disk YAML parsing (`loader.load()`) on every single player connection on the Netty worker event loop. Velolist maintains an atomic, thread-safe in-memory cache (`ConcurrentHashMap`), reducing whitelist lookup complexity to $O(1)$ with zero synchronous disk I/O on join.
- **Full Bedrock / Floodgate Crossplay Support:** Native resolution for Bedrock players using the `.` prefix via direct Floodgate API integration, supporting Bedrock Gamertags and Floodgate UUIDs without failing against Java Mojang API lookups.
- **Thread-Safe Atomic Config IO:** Replaced asynchronous uncoordinated file writes with synchronized persistence handlers, eliminating race conditions and YAML file corruption from concurrent admin commands.
- **Resilient API Timeouts & Reverse Name Cache:** All external UUID/username lookups (via `playerdb.co` and Floodgate) enforce strict 5-second connect and read timeouts, preventing worker thread exhaustion. Added in-memory name caching and online proxy player resolution so `/vlist list` loads instantly.
- **Removed Dangerous / Arbitrary Command Execution:** Stripped broken and unvalidated console command execution (`passthru`) from the command dispatcher.

## Features

- **UUID-Based Whitelisting:** Tracks players by persistent UUIDs rather than mutable usernames.
- **Crossplay Ready (Java & Bedrock):** Seamlessly whitelist Bedrock players by Gamertag (`.Gamertag`) or Floodgate UUID.
- **Ultra-Low Latency:** In-memory lookups ensure zero proxy tick lag or connection bottlenecking during high-volume join spikes.
- **Live Whitelist Toggle:** Instantly toggle whitelist enforcement (`/vlist on` and `/vlist off`) without proxy restarts.
- **Paginated Management:** Clean, paginated `/vlist list` command with interactive click-to-copy UUID components.
- **Hot Reloading:** Safely reload disk configuration and in-memory caches on the fly with `/vlist reload`.
- **Backward-Compatible Permissions & Aliases:** Supports `/velolist`, `/vlist`, and legacy `/vitelist` commands, with both `velolist.*` and `vitelist.*` permission node parity.

## Requirements

- **Proxy:** [Velocity](https://papermc.io/software/velocity) 3.3.0+ / 3.4.0+
- **Java:** Java 21+
- **Optional Dependencies:** [Floodgate](https://geysermc.org/download#floodgate) (required only for Bedrock Gamertag prefix resolution)

## Installation

1. Download the latest `velolist-2.1.0.jar` from [Releases](https://github.com/Dxrmy/velolist/releases).
2. Place `velolist-2.1.0.jar` into your Velocity proxy's `plugins/` directory.
3. Restart or start your Velocity proxy to generate `plugins/vitelist/config.yml`.

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/vlist on` | `velolist.on` | Enables whitelist enforcement proxy-wide. |
| `/vlist off` | `velolist.off` | Disables whitelist enforcement. |
| `/vlist add <player>` | `velolist.add` | Adds a Java player or Bedrock player (`.Name`) to the whitelist. |
| `/vlist remove <player\|uuid>` | `velolist.remove` | Removes a player or UUID from the whitelist. |
| `/vlist list [page]` | `velolist.list` | Displays paginated list of whitelisted players. |
| `/vlist reload` | `velolist.reload` | Reloads `config.yml` into in-memory cache. |

> Legacy permission nodes (`vitelist.admin`, `vitelist.*`, `vitelist.add`, etc.) are retained for backward compatibility.

```yaml
permissions:
  velolist.*:
    description: Full administrative access to Velolist commands
    default: op
  velolist.add:
    description: Allows adding players to the whitelist
    default: op
  velolist.remove:
    description: Allows removing players from the whitelist
    default: op
  velolist.on:
    description: Allows enabling the whitelist
    default: op
  velolist.off:
    description: Allows disabling the whitelist
    default: op
  velolist.list:
    description: Allows viewing the whitelisted players list
    default: op
  velolist.reload:
    description: Allows reloading whitelist configuration
    default: op
```

## Configuration

Settings are stored in `plugins/vitelist/config.yml`:

```yaml
# Whether whitelist enforcement is active
whitelist-enabled: true

# Whitelisted player UUIDs
whitelisted-uuids:
  - "069a79f4-44e9-4726-a5be-fca90e38aaf5"
```

## Building from Source

```bash
git clone https://github.com/Dxrmy/velolist.git
cd velolist
mvn clean package
```

The compiled shaded jar will be generated in `target/velolist-2.1.0.jar`.

## Credits & Upstream

- Original plugin: [Vitelist](https://github.com/0PandaDEV/Vitelist) by [PandaDEV](https://pandadev.net).
- Forked & maintained by [Dxrmy](https://github.com/Dxrmy) with performance caching, Bedrock Floodgate support, and security hardening.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
