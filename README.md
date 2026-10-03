# Velolist

A simple, fast, and secure whitelist plugin for Velocity proxies with Bedrock (Floodgate) support.

Forked from [PandaDEV's Vitelist](https://github.com/0PandaDEV/Vitelist) to add in-game command management and improve reliability.

## Features

- In-game command management (`/vlist add`, `/vlist remove`, `/vlist check`, etc.)
- Bedrock / Floodgate support using `.Name` prefixes to prevent Java username collisions
- In-memory whitelist cache for instant authentication with zero disk I/O on joins
- Platform badges (`[Java]` / `[Bedrock]`) in list and check views
- Bypass permission (`velolist.bypass`) for staff and admins
- Configurable kick message supporting MiniMessage and legacy color codes
- Context-aware tab completion for subcommands and player names

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/vlist on` | `velolist.on` | Turn the whitelist on. |
| `/vlist off` | `velolist.off` | Turn the whitelist off. |
| `/vlist add <player\|uuid>` | `velolist.add` | Add a Java (`Name`) or Bedrock (`.Name`) player. |
| `/vlist remove <player\|uuid>` | `velolist.remove` | Remove a player from the whitelist. |
| `/vlist check <player\|uuid>` | `velolist.check` | Check if a player or UUID is on the whitelist. |
| `/vlist list [page]` | `velolist.list` | View whitelisted players with platform badges. |
| `/vlist status` | `velolist.user` | View current whitelist status and count. |
| `/vlist reload` | `velolist.reload` | Reload the whitelist and config from disk. |

> Aliases: `/velolist`, `/vlist`.

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
  velolist.check:
    description: Allows checking whitelist status of a player
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
  velolist.bypass:
    description: Allows bypassing the proxy whitelist
    default: false
```

## Configuration

`plugins/velolist/config.yml`:

```yaml
whitelist-enabled: true
kick-message: "&cYou are not whitelisted on this server."
whitelisted-uuids: []
```

## Requirements

- **Proxy:** Velocity 3.3.0+
- **Java:** Java 21+
- **Optional:** [Floodgate](https://geysermc.org/download#floodgate) (only needed for Bedrock player names)

## Installation

1. Download `velolist-2.2.0.jar` from [Releases](https://github.com/Dxrmy/velolist/releases).
2. Drop it into your Velocity `plugins/` folder.
3. Start or restart your proxy.

## Building from Source

```bash
git clone https://github.com/Dxrmy/velolist.git
cd velolist
mvn clean package
```

The compiled jar will be located in `target/velolist-2.2.0.jar`.

## License

MIT License. See [LICENSE](LICENSE) for details.
