package me.sunmc.dodgeball.managers.world;

import me.sunmc.dodgeball.DodgeballPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Handles world creation and deletion for dodgeball arenas.
 * Updated for Paper 1.21.1 world management API.
 */
public class WorldManager {

    private final DodgeballPlugin plugin;

    public WorldManager(@NotNull DodgeballPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a new void world with default game settings.
     *
     * @param name World name
     * @return Created world
     */
    public World setupModifiedWorld(@NotNull String name) {
        // Check if world already exists
        World existingWorld = Bukkit.getWorld(name);
        if (existingWorld != null) {
            return existingWorld;
        }

        // Create world with custom generator
        WorldCreator creator = new WorldCreator(name);
        creator.generator(new EmptyChunkGenerator());
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);

        World world = creator.createWorld();

        if (world == null) {
            plugin.getLogger().severe("Failed to create world: " + name);
            return null;
        }

        // Apply game settings
        this.applyGameSettings(world);

        return world;
    }

    /**
     * Applies default game settings to a world.
     *
     * @param world World to configure
     */
    private void applyGameSettings(@NotNull World world) {
        world.setAutoSave(true);
        world.setStorm(false);
        world.setWeatherDuration(0);
        world.setThunderDuration(0);
        world.setPVP(false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_MOB_LOOT, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setFullTime(1500); // Fixed at noon
    }

    /**
     * Unloads and deletes a world.
     *
     * @param name World name
     */
    public void deleteAndUnloadWorld(@NotNull String name) {
        World world = Bukkit.getWorld(name);
        if (world == null) {
            return;
        }

        // Kick all players from the world
        for (Player player : world.getPlayers()) {
            player.kick(Component.text("World is being deleted"));
        }

        // Unload world
        Bukkit.unloadWorld(world, false);

        // Delete world folder asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            this.deleteWorldFolder(world.getWorldFolder().toPath());
        });
    }

    /**
     * Recursively deletes a world folder.
     *
     * @param path Path to world folder
     */
    private void deleteWorldFolder(@NotNull Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to delete world folder: " + path);
            exception.printStackTrace();
        }
    }
}