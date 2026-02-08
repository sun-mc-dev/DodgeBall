package me.sunmc.dodgeball.component;

import me.sunmc.dodgeball.DodgeBall;
import me.sunmc.dodgeball.arena.Arena;
import me.sunmc.dodgeball.game.PlayMode;
import me.sunmc.dodgeball.player.DodgeBallPlayer;
import me.sunmc.tools.Tools;
import me.sunmc.tools.component.Component;
import me.sunmc.tools.configuration.ConfigurationProvider;
import me.sunmc.tools.registry.AutoRegister;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all arenas - FULLY IMPLEMENTED
 */
@AutoRegister(Component.class)
public class ArenaManager implements Component {

    private final @NonNull DodgeBall plugin;
    private final @NonNull Map<String, Arena> arenas;
    private final @NonNull Map<UUID, Arena> playerArenas;

    public ArenaManager(@NonNull DodgeBall plugin) {
        this.plugin = plugin;
        this.arenas = new ConcurrentHashMap<>();
        this.playerArenas = new ConcurrentHashMap<>();
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("Loading arenas from configuration...");
        loadArenas();
        plugin.getLogger().info("Loaded " + arenas.size() + " arenas");
    }

    @Override
    public void onDisable() {
        // Save all arenas
        saveArenas();

        // Remove all players from arenas
        new ArrayList<>(playerArenas.keySet()).forEach(this::removePlayer);
        arenas.clear();
    }

    /**
     * Loads all arenas from configuration
     */
    private void loadArenas() {
        Optional<ConfigurationProvider> configOpt = plugin.getRegisteredConfig("arenas");
        if (configOpt.isEmpty()) {
            plugin.getLogger().warning("arenas.yml not found!");
            return;
        }

        ConfigurationProvider config = configOpt.get();
        ConfigurationNode arenasNode = config.getNode("arenas");

        if (arenasNode.virtual()) {
            plugin.getLogger().info("No arenas configured yet");
            return;
        }

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : arenasNode.childrenMap().entrySet()) {
            String arenaId = String.valueOf(entry.getKey());
            ConfigurationNode arenaNode = entry.getValue();

            try {
                Arena arena = loadArenaFromNode(arenaId, arenaNode);
                arenas.put(arenaId, arena);
                plugin.getLogger().info("Loaded arena: " + arenaId);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load arena: " + arenaId);
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads a single arena from configuration node
     */
    private @NonNull Arena loadArenaFromNode(@NonNull String arenaId, @NonNull ConfigurationNode node) {
        String displayName = node.node("display-name").getString(arenaId);
        String modeStr = node.node("game-mode").getString("CLASSIC");
        int minPlayers = node.node("min-players").getInt(2);
        int maxPlayers = node.node("max-players").getInt(10);

        PlayMode mode = PlayMode.valueOf(modeStr.toUpperCase());
        Arena arena = new Arena(arenaId, displayName, minPlayers, maxPlayers, mode);

        // Load locations
        ConfigurationNode locationsNode = node.node("locations");
        if (!locationsNode.virtual()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> locEntry : locationsNode.childrenMap().entrySet()) {
                String locType = String.valueOf(locEntry.getKey()).toUpperCase().replace("-", "_");
                ConfigurationNode locNode = locEntry.getValue();

                String worldName = locNode.node("world").getString("world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                double x = locNode.node("x").getDouble(0);
                double y = locNode.node("y").getDouble(64);
                double z = locNode.node("z").getDouble(0);
                float yaw = (float) locNode.node("yaw").getDouble(0);
                float pitch = (float) locNode.node("pitch").getDouble(0);

                Location location = new Location(world, x, y, z, yaw, pitch);
                arena.setLocation(locType, location);
            }
        }

        // Load settings
        ConfigurationNode settingsNode = node.node("settings");
        if (!settingsNode.virtual()) {
            arena.getSettings().setAllowPowerUps(settingsNode.node("allow-powerups").getBoolean(true));
            arena.getSettings().setAllowRespawn(settingsNode.node("allow-respawn").getBoolean(false));
            arena.getSettings().setBallDamage(settingsNode.node("ball-damage").getInt(4));
            arena.getSettings().setBallSpeed(settingsNode.node("ball-speed").getDouble(1.5));
            arena.getSettings().setGameDuration(settingsNode.node("game-duration").getInt(300));
        }

        return arena;
    }

    /**
     * Saves all arenas to configuration
     */
    private void saveArenas() {
        Optional<ConfigurationProvider> configOpt = plugin.getRegisteredConfig("arenas");
        if (configOpt.isEmpty()) return;

        ConfigurationProvider config = configOpt.get();

        for (Arena arena : arenas.values()) {
            saveArenaToConfig(arena, config);
        }

        try {
            config.save();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save arenas!");
            e.printStackTrace();
        }
    }

    /**
     * Saves a single arena to configuration
     */
    private void saveArenaToConfig(@NonNull Arena arena, @NonNull ConfigurationProvider config) {
        String path = "arenas." + arena.getArenaId();

        config.set(arena.getDisplayName(), path + ".display-name");
        config.set(arena.getGameMode().name(), path + ".game-mode");
        config.set(arena.getMinPlayers(), path + ".min-players");
        config.set(arena.getMaxPlayers(), path + ".max-players");

        // Save locations
        for (String locType : List.of("LOBBY", "CENTER", "TEAM1_SPAWN", "TEAM2_SPAWN", "SPECTATOR", "MIN_BOUND", "MAX_BOUND")) {
            Location loc = arena.getLocation(locType);
            if (loc != null) {
                String locPath = path + ".locations." + locType.toLowerCase().replace("_", "-");
                config.set(loc.getWorld().getName(), locPath + ".world");
                config.set(loc.getX(), locPath + ".x");
                config.set(loc.getY(), locPath + ".y");
                config.set(loc.getZ(), locPath + ".z");
                config.set(loc.getYaw(), locPath + ".yaw");
                config.set(loc.getPitch(), locPath + ".pitch");
            }
        }

        // Save settings
        config.set(arena.getSettings().isAllowPowerUps(), path + ".settings.allow-powerups");
        config.set(arena.getSettings().isAllowRespawn(), path + ".settings.allow-respawn");
        config.set(arena.getSettings().getBallDamage(), path + ".settings.ball-damage");
        config.set(arena.getSettings().getBallSpeed(), path + ".settings.ball-speed");
        config.set(arena.getSettings().getGameDuration(), path + ".settings.game-duration");
    }

    /**
     * Reloads all arenas from configuration
     */
    public void reloadArenas() {
        // Save current arenas first
        saveArenas();

        // Clear and reload
        arenas.clear();
        loadArenas();
    }

    public void createArena(@NonNull String id, @NonNull String name, int min, int max, @NonNull PlayMode mode) {
        Arena arena = new Arena(id, name, min, max, mode);
        arenas.put(id, arena);

        // Save to config
        Optional<ConfigurationProvider> configOpt = plugin.getRegisteredConfig("arenas");
        configOpt.ifPresent(config -> {
            saveArenaToConfig(arena, config);
            try {
                config.save();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save new arena!");
            }
        });
    }

    public boolean deleteArena(@NonNull String id) {
        Arena arena = arenas.remove(id);
        if (arena == null) return false;

        // Remove all players
        new ArrayList<>(arena.getPlayers()).forEach(p -> removePlayer(p.getUuid()));

        // Remove from config
        Optional<ConfigurationProvider> configOpt = plugin.getRegisteredConfig("arenas");
        configOpt.ifPresent(config -> {
            config.set(null, "arenas." + id);
            try {
                config.save();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to delete arena from config!");
            }
        });

        return true;
    }

    public @Nullable Arena getArena(@NonNull String id) {
        return arenas.get(id);
    }

    public @NonNull Collection<Arena> getArenas() {
        return new ArrayList<>(arenas.values());
    }

    public @Nullable Arena getPlayerArena(@NonNull Player player) {
        return playerArenas.get(player.getUniqueId());
    }

    public boolean addPlayer(@NonNull Player player, @NonNull Arena arena) {
        DodgeBallPlayer dbPlayer = Tools.getComponent(me.sunmc.dodgeball.component.PlayerManager.class).getPlayer(player);

        if (arena.addPlayer(dbPlayer)) {
            playerArenas.put(player.getUniqueId(), arena);
            return true;
        }
        return false;
    }

    public void removePlayer(@NonNull UUID playerId) {
        Arena arena = playerArenas.remove(playerId);
        if (arena != null) {
            DodgeBallPlayer dbPlayer = Tools.getComponent(me.sunmc.dodgeball.component.PlayerManager.class).getPlayer(playerId);
            if (dbPlayer != null) {
                arena.removePlayer(dbPlayer);
            }
        }
    }
}