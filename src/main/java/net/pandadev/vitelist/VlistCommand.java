package net.pandadev.vitelist;

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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        if (!source.hasPermission("velolist.*") && !source.hasPermission("vitelist.*") && !source.hasPermission("velolist.user") && !source.hasPermission("vitelist.user")) {
            source.sendMessage(Component.text(Main.getPrefix() + "§cYou don't have permission to use this command"));
            return;
        }
        if (args.length < 1) {
            source.sendMessage(Component.text(Main.getPrefix() + "§cInvalid Usage! Available commands: add, remove, on, off, list, reload"));
            return;
        }
        switch (args[0].toLowerCase()) {
            case "add":
                handleAdd(source, args);
                break;
            case "remove":
                handleRemove(source, args);
                break;
            case "on":
                if (!source.hasPermission("velolist.on") && !source.hasPermission("vitelist.on") && !source.hasPermission("velolist.*") && !source.hasPermission("vitelist.*")) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§cYou don't have permission to use this command"));
                    return;
                }
                plugin.setWhitelistEnabled(true);
                source.sendMessage(Component.text(Main.getPrefix() + "§7Whitelist enabled"));
                break;
            case "off":
                if (!source.hasPermission("velolist.off") && !source.hasPermission("vitelist.off") && !source.hasPermission("velolist.*") && !source.hasPermission("vitelist.*")) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§cYou don't have permission to use this command"));
                    return;
                }
                plugin.setWhitelistEnabled(false);
                source.sendMessage(Component.text(Main.getPrefix() + "§7Whitelist disabled"));
                break;
            case "list":
                handleList(source, args);
                break;
            case "reload":
                if (!source.hasPermission("velolist.reload") && !source.hasPermission("vitelist.reload") && !source.hasPermission("velolist.*") && !source.hasPermission("vitelist.*")) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§cYou don't have permission to use this command"));
                    return;
                }
                if (plugin.reloadConfig()) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§aConfiguration reloaded successfully."));
                } else {
                    source.sendMessage(Component.text(Main.getPrefix() + "§cFailed to reload configuration. Check proxy console."));
                }
                break;
            case "passthru":
                source.sendMessage(Component.text(Main.getPrefix() + "§cThe passthru command has been removed for security."));
                break;
            default:
                source.sendMessage(Component.text(Main.getPrefix() + "§cInvalid command. Available: add, remove, on, off, list, reload"));
        }
    }

    private void handleAdd(CommandSource source, String[] args) {
        if (!source.hasPermission("velolist.add") && !source.hasPermission("vitelist.add") && !source.hasPermission("velolist.*") && !source.hasPermission("vitelist.*")) {
            source.sendMessage(Component.text(Main.getPrefix() + "§cYou don't have permission to use this command"));
            return;
        }
        if (args.length < 2) {
            source.sendMessage(Component.text(Main.getPrefix() + "§cPlease specify a player name to add"));
            return;
        }
        CompletableFuture.runAsync(() -> {
            final String name = args[1];
            try {
                String uuid = getUUID(name);
                if (uuid == null) {
                    if (isValidUUID(name)) {
                        uuid = name;
                    } else {
                        source.sendMessage(Component.text(Main.getPrefix() + "§cCould not find UUID for player name " + name));
                        return;
                    }
                }
                uuid = uuid.toLowerCase();
                nameCache.put(uuid, name);

                boolean added = plugin.addUuid(uuid);
                Component playerNameComponent = getNameFromUUID(uuid);
                if (added) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§7Added §a").append(playerNameComponent).append(Component.text(" §7to the whitelist")));
                } else {
                    source.sendMessage(Component.text(Main.getPrefix() + "§a").append(playerNameComponent).append(Component.text(" §7is already on the whitelist")));
                }
            } catch (Exception e) {
                source.sendMessage(Component.text(Main.getPrefix() + "§cAn error occurred while processing the command: " + e.getMessage()));
            }
        });
    }

    private void handleRemove(CommandSource source, String[] args) {
        if (!source.hasPermission("velolist.remove") && !source.hasPermission("vitelist.remove") && !source.hasPermission("velolist.*") && !source.hasPermission("vitelist.*")) {
            source.sendMessage(Component.text(Main.getPrefix() + "§cYou don't have permission to use this command"));
            return;
        }
        if (args.length < 2) {
            source.sendMessage(Component.text(Main.getPrefix() + "§cPlease specify a player name to remove"));
            return;
        }
        final String name = args[1];
        CompletableFuture.runAsync(() -> {
            try {
                String uuid = getUUID(name);
                if (uuid == null) {
                    if (isValidUUID(name)) {
                        uuid = name;
                    } else {
                        source.sendMessage(Component.text(Main.getPrefix() + "§cCould not find UUID for player name " + name));
                        return;
                    }
                }
                uuid = uuid.toLowerCase();
                Component playerNameComponent = getNameFromUUID(uuid);
                boolean removed = plugin.removeUuid(uuid);
                if (removed) {
                    source.sendMessage(Component.text(Main.getPrefix() + "§7Removed §a").append(playerNameComponent).append(Component.text(" §7from the whitelist")));
                } else {
                    source.sendMessage(Component.text(Main.getPrefix() + "§a").append(playerNameComponent).append(Component.text(" §7not found on the whitelist")));
                }
            } catch (Exception e) {
                plugin.getLogger().error("Error in remove command", e);
                source.sendMessage(Component.text(Main.getPrefix() + "§cAn error occurred: " + e.getMessage()));
            }
        });
    }

    private void handleList(CommandSource source, String[] args) {
        if (!source.hasPermission("velolist.list") && !source.hasPermission("vitelist.list") && !source.hasPermission("velolist.*") && !source.hasPermission("vitelist.*")) {
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

        // 1. Check in-memory cache
        String cachedName = nameCache.get(uuid);
        if (cachedName != null) {
            return Component.text("§7" + cachedName);
        }

        // 2. Check online players on the proxy
        try {
            Optional<Player> onlineOpt = plugin.getServer().getPlayer(UUID.fromString(uuid));
            if (onlineOpt.isPresent()) {
                String name = onlineOpt.get().getUsername();
                nameCache.put(uuid, name);
                return Component.text("§7" + name);
            }
        } catch (Exception ignored) {}

        // 3. Check Floodgate API for Bedrock player
        try {
            FloodgateApi floodgateApi = FloodgateApi.getInstance();
            if (floodgateApi != null) {
                var fPlayer = floodgateApi.getPlayer(UUID.fromString(uuid));
                if (fPlayer != null) {
                    String name = "." + fPlayer.getUsername();
                    nameCache.put(uuid, name);
                    return Component.text("§7" + name);
                }
            }
        } catch (Throwable ignored) {}

        // 4. Query playerdb.co with timeout
        try {
            URL url = new URL("https://playerdb.co/api/player/minecraft/" + uuid);
            String playerName = getPlayerNameFromAPI(url);
            if (playerName != null) {
                nameCache.put(uuid, playerName);
                return Component.text("§7" + playerName);
            }
        } catch (Exception ignored) {}

        return Component.text("§c" + uuid)
                .clickEvent(ClickEvent.copyToClipboard(uuid))
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy UUID")))
                .append(Component.text(" §8(§7UUID§8)"));
    }

    private static boolean isValidUUID(String uuid) {
        if (uuid == null) return false;
        return uuid.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    public static String getUUID(String name) {
        // Check for Floodgate prefix
        if (name.startsWith(".")) {
            try {
                FloodgateApi api = FloodgateApi.getInstance();
                if (api != null) {
                    UUID uuid = api.getUuidFor(name).get(5, TimeUnit.SECONDS);
                    if (uuid != null) {
                        return uuid.toString().toLowerCase();
                    }
                }
            } catch (Exception e) {
                System.out.println("Floodgate lookup failed for: " + name + " - " + e.getMessage());
            }
            // Fallback: try as direct UUID (strip prefix first)
            String stripped = name.substring(1);
            if (isValidUUID(stripped)) {
                return stripped.toLowerCase();
            }
            System.out.println("Unable to resolve Floodgate player: " + name);
            return null;
        }

        try {
            URL url = new URL("https://playerdb.co/api/player/minecraft/" + name);
            return getPlayerUUIDFromAPI(url);
        } catch (Exception e) {
            System.out.println("Unable to get UUID for: " + name + " due to error: " + e.getMessage());
        }
        return null;
    }

    private static String getPlayerNameFromAPI(URL url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(HTTP_TIMEOUT_MS);
        conn.setReadTimeout(HTTP_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "Velolist/2.1.0");
        conn.connect();
        int responseCode = conn.getResponseCode();
        if (responseCode == 400 || responseCode == 404) return null;
        if (responseCode != 200) throw new RuntimeException("HttpResponseCode: " + responseCode);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            if (!jsonObject.has("data")) return null;
            JsonObject data = jsonObject.getAsJsonObject("data");
            if (!data.has("player")) return null;
            JsonObject player = data.getAsJsonObject("player");
            return player.get("username").getAsString();
        }
    }

    private static String getPlayerUUIDFromAPI(URL url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(HTTP_TIMEOUT_MS);
        conn.setReadTimeout(HTTP_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "Velolist/2.1.0");
        conn.connect();
        int responseCode = conn.getResponseCode();
        if (responseCode == 400 || responseCode == 404) return null;
        if (responseCode != 200) throw new RuntimeException("HttpResponseCode: " + responseCode);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            if (!jsonObject.has("data")) return null;
            JsonObject data = jsonObject.getAsJsonObject("data");
            if (!data.has("player")) return null;
            JsonObject player = data.getAsJsonObject("player");
            return player.get("id").getAsString();
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0 || (args.length == 1 && args[0].isEmpty())) {
            return Stream.of("add", "remove", "on", "off", "list", "reload").collect(Collectors.toList());
        } else if (args.length == 1) {
            return Stream.of("add", "remove", "on", "off", "list", "reload")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) return Stream.of("<player>").collect(Collectors.toList());
            if (args[0].equalsIgnoreCase("remove")) return Stream.of("<player/uuid>").collect(Collectors.toList());
        }
        return List.of();
    }
}
