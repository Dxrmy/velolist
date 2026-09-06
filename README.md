# Velolist

A command-driven, security-hardened whitelist plugin for Velocity proxies with Bedrock (Floodgate) support.

Forked from [PandaDEV's Vitelist](https://github.com/0PandaDEV/Vitelist) to introduce full in-game management commands and patch critical security and performance vulnerabilities.

## Why Velolist?

Upstream Vitelist lacked interactive command-line management and suffered from several architectural vulnerabilities under proxy load. Velolist addresses this directly:

- **Full Command Management:** Adds complete command control (`/vlist add`, `/vlist remove`, `/vlist on`, `/vlist off`, `/vlist list`, `/vlist reload`) so admins can manage proxy whitelists live without manual config editing or proxy restarts.
- **Bedrock / Floodgate Support:** Whitelist Bedrock crossplay players seamlessly using either their Floodgate prefix (`.PlayerName`) or Floodgate UUID.
- **Fail-Closed Security Patch:** Upstream caught configuration read errors and failed open, allowing unwhitelisted players to bypass the whitelist if disk I/O lagged. Velolist intercepts connections at `LoginEvent` and strictly fails closed.
- **In-Memory Caching (Zero Netty Thread Lag):** Upstream parsed YAML synchronously on every connection inside Netty worker loops. Velolist stores whitelisted UUIDs in an in-memory concurrent set for $O(1)$ lookups with zero disk I/O on joins.
- **API Request Timeouts:** Enforces 5-second timeouts and local name caching on external playerdb/Floodgate lookups, preventing proxy worker threads from stalling if upstream APIs hang.
- **Thread-Safe Persistence:** Uses synchronized file handlers to eliminate race conditions and corrupted YAML files during simultaneous admin commands.

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/vlist on` | `velolist.on` | Enables whitelist enforcement proxy-wide. |
| `/vlist off` | `velolist.off` | Disables whitelist enforcement. |
| `/vlist add <player>` | `velolist.add` | Adds a Java or Bedrock (`.Name`) player to the whitelist. |
| `/vlist remove <player\|uuid>` | `velolist.remove` | Removes a player or UUID from the whitelist. |
| `/vlist list [page]` | `velolist.list` | Displays paginated list of whitelisted players. |
| `/vlist reload` | `velolist.reload` | Reloads configuration from disk into memory. |

> Aliases: `/velolist`, `/vlist`, `/vitelist`. Legacy `vitelist.*` permission nodes are supported for backward compatibility.

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

## Requirements

- **Proxy:** Velocity 3.3.0+ / 3.4.0+
- **Java:** Java 21+
- **Optional:** [Floodgate](https://geysermc.org/download#floodgate) for Bedrock player prefix resolution

## Installation

1. Download `velolist-2.1.0.jar` from [Releases](https://github.com/Dxrmy/velolist/releases).
2. Place into your Velocity proxy's `plugins/` directory.
3. Start or restart your proxy.

## Building from Source

```bash
git clone https://github.com/Dxrmy/velolist.git
cd velolist
mvn clean package
```

The shaded jar will be built at `target/velolist-2.1.0.jar`.

## License

MIT License. See [LICENSE](LICENSE) for details.
