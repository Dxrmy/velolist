package me.dxrmy.velolist;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.geysermc.floodgate.api.FloodgateApi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class VlistCommand implements SimpleCommand {

    private static final int HTTP_TIMEOUT_MS = 5000;
    private static final Map<String, String> nameCache = new ConcurrentHashMap<>();

    private final Main plugin;

    public VlistCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1 || args[0].equalsIgnoreCase("status")) {
            if (!source.hasPermission("velolist.*") && !source.hasPermission("velolist.user")) {
                source.sendMessage(Component.text(Main.getPrefix() + "§cYou don't have permission to use this command"));
                return;
            }
            String status = plugin.isWhitelistEnabled() ? "§aENABLED" : "§cDISABLED";
            source.sendMessage(Component.text(Main.getPrefix() + "§7Whitelist status: " + status + " §8(§e" + plugin.getWhitelistedUuids().size() + " §7whitelisted§8)"));
            source.sendMessage(Component.text(Main.getPrefix() + "§7Commands: §d/vlist <on|off|add|remove|check|list|reload>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                handleAdd(source, args);
                break;
            case "remove":
                handleRemove(source, args);
                break;
            case "check":
                handleCheck(source, args);
                break;
            case "on":
                if (!source.hasPermission("velolist.on") && !source.hasPermission("velolist.*")) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§cYou don't have permission to use this command"));
                    return;
                }
                plugin.setWhitelistEnabled(true);
                source.sendMessage(Component.text(Main.getPrefix() + "§7Whitelist §aenabled"));
                break;
            case "off":
                if (!source.hasPermission("velolist.off") && !source.hasPermission("velolist.*")) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§cYou don't have permission to use this command"));
                    return;
                }
                plugin.setWhitelistEnabled(false);
                source.sendMessage(Component.text(Main.getPrefix() + "§7Whitelist §cdisabled"));
                break;
            case "list":
                handleList(source, args);
                break;
            case "reload":
                if (!source.hasPermission("velolist.reload") && !source.hasPermission("velolist.*")) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§cYou don't have permission to use this command"));
                    return;
                }
                if (plugin.reloadConfig()) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§aConfiguration reloaded successfully."));
                } else {
                    source.sendMessage(Component.text(Main.getPrefix() + "§cFailed to reload configuration. Check proxy console."));
                }
                break;
            default:
                source.sendMessage(Component.text(Main.getPrefix() + "§cInvalid command. Available: add, remove, check, on, off, list, reload, status"));
        }
    }

    private void handleAdd(CommandSource source, String[] args) {
        if (!source.hasPermission("velolist.add") && !source.hasPermission("velolist.*")) {
            source.sendMessage(Component.text(Main.getPrefix() + "§cYou don't have permission to use this command"));
            return;
        }
        if (args.length < 2) {
            source.sendMessage(Component.text(Main.getPrefix() + "§cPlease specify a player name or UUID to add"));
            return;
        }
        final String input = args[1];
        CompletableFuture.runAsync(() -> {
            try {
                String uuid = resolveUUID(input);
                if (uuid == null) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§cCould not resolve UUID for '" + input + "'."));
                    return;
                }
                boolean added = plugin.addUuid(uuid);
                Component playerNameComponent = getNameFromUUID(uuid);
                if (added) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§7Added ").append(playerNameComponent).append(Component.text(" §7to the whitelist")));
                } else {
                    source.sendMessage(Component.text(Main.getPrefix()).append(playerNameComponent).append(Component.text(" §7is already on the whitelist")));
                }
            } catch (Exception e) {
                source.sendMessage(Component.text(Main.getPrefix() + "§cAn error occurred while processing: " + e.getMessage()));
            }
        });
    }

    private void handleRemove(CommandSource source, String[] args) {
        if (!source.hasPermission("velolist.remove") && !source.hasPermission("velolist.*")) {
            source.sendMessage(Component.text(Main.getPrefix() + "§cYou don't have permission to use this command"));
            return;
        }
        if (args.length < 2) {
            source.sendMessage(Component.text(Main.getPrefix() + "§cPlease specify a player name or UUID to remove"));
            return;
        }
        final String input = args[1];
        CompletableFuture.runAsync(() -> {
            try {
                String uuid = resolveUUID(input);
                if (uuid == null) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§cCould not resolve UUID for '" + input + "'."));
                    return;
                }
                Component playerNameComponent = getNameFromUUID(uuid);
                boolean removed = plugin.removeUuid(uuid);
                if (removed) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§7Removed ").append(playerNameComponent).append(Component.text(" §7from the whitelist")));
                } else {
                    source.sendMessage(Component.text(Main.getPrefix()).append(playerNameComponent).append(Component.text(" §7not found on the whitelist")));
                }
            } catch (Exception e) {
                plugin.getLogger().error("Error in remove command", e);
                source.sendMessage(Component.text(Main.getPrefix() + "§cAn error occurred: " + e.getMessage()));
            }
        });
    }

    private void handleCheck(CommandSource source, String[] args) {
        if (!source.hasPermission("velolist.check") && !source.hasPermission("velolist.*")) {
            source.sendMessage(Component.text(Main.getPrefix() + "§cYou don't have permission to use this command"));
            return;
        }
        if (args.length < 2) {
            source.sendMessage(Component.text(Main.getPrefix() + "§cPlease specify a player name or UUID to check"));
            return;
        }
        final String input = args[1];
        CompletableFuture.runAsync(() -> {
            try {
                String uuid = resolveUUID(input);
                if (uuid == null) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§cCould not resolve UUID for '" + input + "'."));
                    return;
                }
                boolean whitelisted = plugin.getWhitelistedUuids().contains(uuid);
                Component playerNameComponent = getNameFromUUID(uuid);
                if (whitelisted) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§7Player ").append(playerNameComponent).append(Component.text(" §7is §awhitelisted§7.")));
                } else {
                    source.sendMessage(Component.text(Main.getPrefix() + "§7Player ").append(playerNameComponent).append(Component.text(" §7is §cNOT §7whitelisted.")));
                }
            } catch (Exception e) {
                plugin.getLogger().error("Error in check command", e);
                source.sendMessage(Component.text(Main.getPrefix() + "§cAn error occurred: " + e.getMessage()));
            }
        });
    }

    private void handleList(CommandSource source, String[] args) {
        if (!source.hasPermission("velolist.list") && !source.hasPermission("velolist.*")) {
            source.sendMessage(Component.text(Main.getPrefix() + "§cYou don't have permission to use this command"));
            return;
        }
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                source.sendMessage(Component.text(Main.getPrefix() + "§cInvalid page number."));
                return;
            }
        }
        final int finalPage = page;
        CompletableFuture.runAsync(() -> {
            try {
                List<String> uuids = new ArrayList<>(plugin.getWhitelistedUuids());
                if (uuids.isEmpty()) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§7No players are currently whitelisted."));
                    return;
                }
                int playersPerPage = 10;
                int totalPages = (int) Math.ceil((double) uuids.size() / playersPerPage);
                if (finalPage < 1 || finalPage > totalPages) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§cInvalid page number. Page must be between 1 and " + totalPages));
                    return;
                }
                int startIndex = (finalPage - 1) * playersPerPage;
                int endIndex = Math.min(startIndex + playersPerPage, uuids.size());
                List<String> pageUuids = uuids.subList(startIndex, endIndex);
                List<Component> playerNames = new ArrayList<>();
                for (String uuid : pageUuids) {
                    playerNames.add(getNameFromUUID(uuid));
                }
                source.sendMessage(Component.text("§8----- [ §d§lWhitelisted players §7(Page " + finalPage + "/" + totalPages + ") §8] -----"));
                source.sendMessage(Component.text(""));
                for (Component player : playerNames) {
                    source.sendMessage(player);
                }
                source.sendMessage(Component.text(""));
                source.sendMessage(Component.text("§8--------------------------------"));
            } catch (Exception e) {
                plugin.getLogger().error("Error in 'list' command", e);
                source.sendMessage(Component.text(Main.getPrefix() + "§cAn error occurred: " + e.getMessage()));
            }
        });
    }

    private Component getNameFromUUID(String rawUuid) {
        if (rawUuid == null) {
            return Component.text("§cUnknown");
        }
        String uuid = rawUuid.toLowerCase();
        if (!isValidUUID(uuid)) {
            return Component.text("§c" + uuid)
                    .clickEvent(ClickEvent.copyToClipboard(uuid))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to copy UUID")))
                    .append(Component.text(" §8(§cINVALID§8)"));
        }

        String cachedName = nameCache.get(uuid);
        if (cachedName != null) {
            boolean isBedrock = cachedName.startsWith(".") || isBedrockUUID(uuid);
            String badge = isBedrock ? "§8[§bBedrock§8] §f" : "§8[§aJava§8] §f";
            return Component.text(badge + cachedName)
                    .clickEvent(ClickEvent.copyToClipboard(cachedName))
                    .hoverEvent(HoverEvent.showText(Component.text("UUID: " + uuid + "\nClick to copy name")));
        }

        try {
            Optional<Player> onlineOpt = plugin.getServer().getPlayer(UUID.fromString(uuid));
            if (onlineOpt.isPresent()) {
                String name = onlineOpt.get().getUsername();
                nameCache.put(uuid, name);
                boolean isBedrock = name.startsWith(".") || isBedrockUUID(uuid);
                String badge = isBedrock ? "§8[§bBedrock§8] §f" : "§8[§aJava§8] §f";
                return Component.text(badge + name)
                        .clickEvent(ClickEvent.copyToClipboard(name))
                        .hoverEvent(HoverEvent.showText(Component.text("UUID: " + uuid + "\nClick to copy name")));
            }
        } catch (Exception ignored) {}

        try {
            FloodgateApi floodgateApi = FloodgateApi.getInstance();
            if (floodgateApi != null) {
                var fPlayer = floodgateApi.getPlayer(UUID.fromString(uuid));
                if (fPlayer != null) {
                    String name = "." + fPlayer.getUsername();
                    nameCache.put(uuid, name);
                    return Component.text("§8[§bBedrock§8] §f" + name)
                            .clickEvent(ClickEvent.copyToClipboard(name))
                            .hoverEvent(HoverEvent.showText(Component.text("UUID: " + uuid + "\nClick to copy name")));
                }
            }
        } catch (Throwable ignored) {}

        JsonObject player = fetchPlayerData(uuid);
        if (player != null && player.has("username")) {
            String playerName = player.get("username").getAsString();
            nameCache.put(uuid, playerName);
            return Component.text("§8[§aJava§8] §f" + playerName)
                    .clickEvent(ClickEvent.copyToClipboard(playerName))
                    .hoverEvent(HoverEvent.showText(Component.text("UUID: " + uuid + "\nClick to copy name")));
        }

        return Component.text("§7" + uuid)
                .clickEvent(ClickEvent.copyToClipboard(uuid))
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy UUID")))
                .append(Component.text(" §8(§7UUID§8)"));
    }

    private boolean isBedrockUUID(String uuid) {
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            if (api != null) {
                return api.isFloodgateId(UUID.fromString(uuid));
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isValidUUID(String uuid) {
        if (uuid == null) return false;
        return uuid.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    public String resolveUUID(String input) {
        if (input == null || input.isBlank()) return null;
        String trimmed = input.trim();

        if (isValidUUID(trimmed)) {
            return trimmed.toLowerCase();
        }

        Optional<Player> online = plugin.getServer().getPlayer(trimmed);
        if (online.isPresent()) {
            String uuid = online.get().getUniqueId().toString().toLowerCase();
            nameCache.put(uuid, online.get().getUsername());
            return uuid;
        }

        if (trimmed.startsWith(".")) {
            try {
                FloodgateApi api = FloodgateApi.getInstance();
                if (api != null) {
                    try {
                        UUID uuid = api.getUuidFor(trimmed).get(5, TimeUnit.SECONDS);
                        if (uuid != null) {
                            String uuidStr = uuid.toString().toLowerCase();
                            nameCache.put(uuidStr, trimmed);
                            return uuidStr;
                        }
                    } catch (Exception ignored) {}

                    String clean = trimmed.substring(1);
                    try {
                        UUID uuid = api.getUuidFor(clean).get(5, TimeUnit.SECONDS);
                        if (uuid != null) {
                            String uuidStr = uuid.toString().toLowerCase();
                            nameCache.put(uuidStr, trimmed);
                            return uuidStr;
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Throwable e) {
                plugin.getLogger().warn("Floodgate lookup failed for {}: {}", trimmed, e.getMessage());
            }

            String stripped = trimmed.substring(1);
            if (isValidUUID(stripped)) {
                return stripped.toLowerCase();
            }

            return null;
        }

        JsonObject player = fetchPlayerData(trimmed);
        if (player != null && player.has("id")) {
            String uuid = player.get("id").getAsString().toLowerCase();
            nameCache.put(uuid, trimmed);
            return uuid;
        }

        return null;
    }

    private static JsonObject fetchPlayerData(String query) {
        try {
            URL url = new URL("https://playerdb.co/api/player/minecraft/" + query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(HTTP_TIMEOUT_MS);
            conn.setReadTimeout(HTTP_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "Velolist/2.2.0");
            conn.connect();

            if (conn.getResponseCode() != 200) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                if (jsonObject != null && jsonObject.has("data")) {
                    JsonObject data = jsonObject.getAsJsonObject("data");
                    if (data != null && data.has("player")) {
                        return data.getAsJsonObject("player");
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("velolist.*") && !source.hasPermission("velolist.user")) {
            return List.of();
        }

        if (args.length == 0 || (args.length == 1 && args[0].isEmpty())) {
            return filterSubcommands(source, "");
        } else if (args.length == 1) {
            return filterSubcommands(source, args[0].toLowerCase());
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();
            if (sub.equals("add") && (source.hasPermission("velolist.add") || source.hasPermission("velolist.*"))) {
                Set<String> whitelisted = plugin.getWhitelistedUuids();
                return plugin.getServer().getAllPlayers().stream()
                        .filter(p -> !whitelisted.contains(p.getUniqueId().toString().toLowerCase()))
                        .map(Player::getUsername)
                        .filter(name -> name.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
            } else if (sub.equals("remove") && (source.hasPermission("velolist.remove") || source.hasPermission("velolist.*"))) {
                List<String> entries = new ArrayList<>();
                for (String uuid : plugin.getWhitelistedUuids()) {
                    String cached = nameCache.get(uuid);
                    entries.add(cached != null ? cached : uuid);
                }
                return entries.stream()
                        .filter(s -> s.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
            } else if (sub.equals("check") && (source.hasPermission("velolist.check") || source.hasPermission("velolist.*"))) {
                List<String> entries = new ArrayList<>();
                for (Player p : plugin.getServer().getAllPlayers()) {
                    entries.add(p.getUsername());
                }
                for (String uuid : plugin.getWhitelistedUuids()) {
                    String cached = nameCache.get(uuid);
                    if (cached != null && !entries.contains(cached)) {
                        entries.add(cached);
                    }
                }
                return entries.stream()
                        .filter(s -> s.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    private List<String> filterSubcommands(CommandSource source, String prefix) {
        List<String> list = new ArrayList<>();
        if (source.hasPermission("velolist.on") || source.hasPermission("velolist.*")) list.add("on");
        if (source.hasPermission("velolist.off") || source.hasPermission("velolist.*")) list.add("off");
        if (source.hasPermission("velolist.add") || source.hasPermission("velolist.*")) list.add("add");
        if (source.hasPermission("velolist.remove") || source.hasPermission("velolist.*")) list.add("remove");
        if (source.hasPermission("velolist.check") || source.hasPermission("velolist.*")) list.add("check");
        if (source.hasPermission("velolist.list") || source.hasPermission("velolist.*")) list.add("list");
        if (source.hasPermission("velolist.reload") || source.hasPermission("velolist.*")) list.add("reload");
        list.add("status");

        if (prefix.isEmpty()) return list;
        return list.stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
    }
}
