package me.dxrmy.velolist;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
        id = "velolist",
        name = "Velolist",
        version = "2.2.0",
        description = "A simple, fast, and secure whitelist plugin for Velocity proxies with Bedrock (Floodgate) support",
        url = "https://github.com/Dxrmy/velolist",
        authors = {"Dxrmy"}
)
public class Main {

    public static String prefix = "§d§lVelolist §8» ";
    @Inject
    private Logger logger;
    @Inject
    private ProxyServer server;
    @Inject
    @DataDirectory
    private Path dataDirectory;
    private YamlConfigurationLoader loader;
    private boolean whitelistEnabled = true;
    private final Set<String> whitelistedUuids = ConcurrentHashMap.newKeySet();
    private Component kickMessage = Component.text("§cYou are not whitelisted on this server.");

    @Inject
    public Main() {
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            if (Files.notExists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to create data directory", e);
        }

        loader = YamlConfigurationLoader.builder().path(dataDirectory.resolve("config.yml")).build();
        loadConfig();

        CommandManager commandManager = server.getCommandManager();
        commandManager.register(
            commandManager.metaBuilder("velolist")
            .aliases("vlist")
            .plugin(this)
            .build(),
            new VlistCommand(this)
        );
    }

    public synchronized void loadConfig() {
        try {
            var root = loader.load();
            if (!root.node("whitelisted-uuids").virtual()) {
                whitelistEnabled = root.node("whitelist-enabled").getBoolean(true);
                String rawKick = root.node("kick-message").getString("&cYou are not whitelisted on this server.");
                kickMessage = parseMessage(rawKick);
                List<String> list = root.node("whitelisted-uuids").getList(String.class);
                whitelistedUuids.clear();
                if (list != null) {
                    for (String u : list) {
                        if (u != null && !u.isBlank()) {
                            whitelistedUuids.add(u.trim().toLowerCase());
                        }
                    }
                }
            } else {
                root.node("whitelisted-uuids").set(List.of());
                root.node("whitelist-enabled").set(true);
                root.node("kick-message").set("&cYou are not whitelisted on this server.");
                loader.save(root);
                whitelistedUuids.clear();
                whitelistEnabled = true;
                kickMessage = parseMessage("&cYou are not whitelisted on this server.");
            }
            logger.info("Velolist loaded. Whitelist enabled: {}, entries: {}", whitelistEnabled, whitelistedUuids.size());
        } catch (ConfigurateException e) {
            logger.error("Failed to load/create config file", e);
        }
    }

    public synchronized boolean reloadConfig() {
        loader = YamlConfigurationLoader.builder().path(dataDirectory.resolve("config.yml")).build();
        try {
            var root = loader.load();
            whitelistEnabled = root.node("whitelist-enabled").getBoolean(true);
            String rawKick = root.node("kick-message").getString("&cYou are not whitelisted on this server.");
            kickMessage = parseMessage(rawKick);
            List<String> list = root.node("whitelisted-uuids").getList(String.class);
            whitelistedUuids.clear();
            if (list != null) {
                for (String u : list) {
                    if (u != null && !u.isBlank()) {
                        whitelistedUuids.add(u.trim().toLowerCase());
                    }
                }
            }
            logger.info("Velolist config reloaded. Whitelist enabled: {}, entries: {}", whitelistEnabled, whitelistedUuids.size());
            return true;
        } catch (ConfigurateException e) {
            logger.error("Failed to reload config", e);
            return false;
        }
    }

    private Component parseMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            return Component.text("You are not whitelisted on this server.");
        }
        if (raw.contains("&") || raw.contains("§")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(raw.replace('§', '&'));
        }
        try {
            return MiniMessage.miniMessage().deserialize(raw);
        } catch (Exception e) {
            return Component.text(raw);
        }
    }

    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
        if (!whitelistEnabled) return;
        Player player = event.getPlayer();
        if (player.hasPermission("velolist.bypass")) return;
        try {
            String uuid = player.getUniqueId().toString().toLowerCase();
            if (!whitelistedUuids.contains(uuid)) {
                event.setResult(ResultedEvent.ComponentResult.denied(kickMessage));
            }
        } catch (Exception e) {
            logger.error("Failed to verify whitelist for " + player.getUsername(), e);
            event.setResult(ResultedEvent.ComponentResult.denied(
                Component.text("§cAn error occurred while verifying your whitelist status. Please reconnect.")
            ));
        }
    }

    public synchronized boolean addUuid(String uuid) {
        String normalized = uuid.toLowerCase();
        try {
            var root = loader.load();
            List<String> rawList = root.node("whitelisted-uuids").getList(String.class);
            List<String> uuids = rawList != null ? new ArrayList<>(rawList) : new ArrayList<>();
            for (String u : uuids) {
                if (u.equalsIgnoreCase(normalized)) {
                    whitelistedUuids.add(normalized);
                    return false;
                }
            }
            uuids.add(normalized);
            root.node("whitelisted-uuids").set(uuids);
            loader.save(root);
            whitelistedUuids.add(normalized);
            return true;
        } catch (ConfigurateException e) {
            logger.error("Failed to save whitelist entry for " + uuid, e);
            throw new RuntimeException("Config save failed: " + e.getMessage(), e);
        }
    }

    public synchronized boolean removeUuid(String uuid) {
        String normalized = uuid.toLowerCase();
        try {
            var root = loader.load();
            List<String> rawList = root.node("whitelisted-uuids").getList(String.class);
            if (rawList == null) return false;
            List<String> uuids = new ArrayList<>(rawList);
            boolean removed = uuids.removeIf(u -> u.equalsIgnoreCase(normalized));
            if (removed) {
                root.node("whitelisted-uuids").set(uuids);
                loader.save(root);
                whitelistedUuids.remove(normalized);
                return true;
            }
            return false;
        } catch (ConfigurateException e) {
            logger.error("Failed to remove whitelist entry for " + uuid, e);
            throw new RuntimeException("Config save failed: " + e.getMessage(), e);
        }
    }

    public synchronized void setWhitelistEnabled(boolean whitelistEnabled) {
        this.whitelistEnabled = whitelistEnabled;
        try {
            var root = loader.load();
            root.node("whitelist-enabled").set(whitelistEnabled);
            loader.save(root);
        } catch (ConfigurateException e) {
            logger.error("Failed to save whitelist-enabled setting", e);
        }
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    public Set<String> getWhitelistedUuids() {
        return Collections.unmodifiableSet(whitelistedUuids);
    }

    public YamlConfigurationLoader getLoader() {
        return loader;
    }

    public ProxyServer getServer() {
        return server;
    }

    public static String getPrefix() {
        return prefix;
    }

    public Logger getLogger() {
        return logger;
    }

    public Component getKickMessage() {
        return kickMessage;
    }
}
