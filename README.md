# Velolist

A simple, fast, and secure whitelist plugin for Velocity proxies with Bedrock (Floodgate) support.

Forked from [PandaDEV's Vitelist](https://github.com/0PandaDEV/Vitelist) to add in-game command management and improve reliability.

## Features

- **Easy In-Game Commands:** Manage your proxy whitelist on the fly (`/vlist add`, `/vlist remove`, etc.) without touching config files or restarting the proxy.
- **Bedrock Crossplay Support:** Whitelist Bedrock players directly by username using your Floodgate prefix (e.g., `/vlist add .Username`).
- **Instant Joins (No Lag):** Keeps the whitelist in memory so player connections are verified instantly with zero lag or stutter.
- **Safe & Reliable:** Fixed security and crash bugs from earlier versions to ensure unwhitelisted players can't slip through and saved files never get corrupted.
- **Interactive List:** Paginated list (`/vlist list`) with click-to-copy UUIDs.

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/vlist on` | `velolist.on` | Turn the whitelist on. |
| `/vlist off` | `velolist.off` | Turn the whitelist off. |
| `/vlist add <player>` | `velolist.add` | Add a Java or Bedrock (`.Name`) player. |
| `/vlist remove <player\|uuid>` | `velolist.remove` | Remove a player from the whitelist. |
| `/vlist list [page]` | `velolist.list` | View all whitelisted players. |
| `/vlist reload` | `velolist.reload` | Reload the whitelist from disk. |

> Aliases: `/velolist`, `/vlist`, `/vitelist`.

```yaml
permissions:
  velolist.*:
    description: Full access to all Velolist commands
    default: op
  velolist.add:
    description: Allows adding players
    default: op
  velolist.remove:
    description: Allows removing players
    default: op
  velolist.on:
    description: Allows enabling the whitelist
    default: op
  velolist.off:
    description: Allows disabling the whitelist
    default: op
  velolist.list:
    description: Allows viewing the whitelist
    default: op
  velolist.reload:
    description: Allows reloading the configuration
    default: op
```

## Requirements

- **Proxy:** Velocity 3.3.0+
- **Java:** Java 21+
- **Optional:** [Floodgate](https://geysermc.org/download#floodgate) (only needed for Bedrock player names)

## Installation

1. Download `velolist-2.1.0.jar` from [Releases](https://github.com/Dxrmy/velolist/releases).
2. Drop it into your Velocity `plugins/` folder.
3. Start or restart your proxy.

## Building from Source

```bash
git clone https://github.com/Dxrmy/velolist.git
cd velolist
mvn clean package
```

The compiled jar will be located in `target/velolist-2.1.0.jar`.

## License

MIT License. See [LICENSE](LICENSE) for details.
