package me.sunmc.dodgeball.listeners;

import me.sunmc.dodgeball.DodgeballPlugin;
import me.sunmc.dodgeball.game.Game;
import me.sunmc.dodgeball.game.setup.GameSetupHelper;
import me.sunmc.dodgeball.managers.ScoreboardManager;
import me.sunmc.dodgeball.storage.GameFileStorage;
import me.sunmc.dodgeball.storage.user.storage.IUserStorage;
import me.sunmc.dodgeball.utility.PermissionHelper;
import me.sunmc.dodgeball.utility.autoregistry.AutoRegister;
import me.sunmc.dodgeball.utility.autoregistry.AutoRegistry;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Lobby-specific event listeners.
 * Modernized for Paper 1.21.1 with Adventure API.
 */
@AutoRegister(type = AutoRegistry.Type.LISTENER)
public class LobbyListener implements Listener {

    private static final String SETTINGS_PATH = "settings.";

    private final DodgeballPlugin plugin;
    private final IUserStorage userManager;
    private final GameFileStorage lobbyConfig;
    private final ScoreboardManager scoreboardManager;

    public LobbyListener(@NotNull DodgeballPlugin plugin) {
        this.plugin = plugin;
        this.userManager = plugin.getUserStorage();
        this.lobbyConfig = plugin.getLobbyConfig();
        this.scoreboardManager = plugin.getScoreboardManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        // Create user if doesn't exist
        this.userManager.getUser(uuid).thenAccept(user -> {
            if (user == null) {
                this.userManager.createNewUser(uuid).thenAccept(newUser ->
                        newUser.setLevel(1)
                );
            }
        });

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            // Apply lobby settings
            if (this.isLobbySettingsEnabled()) {
                this.applyJoinEffects(player);
            }

            // Apply scoreboard
            if (this.scoreboardManager.isLobbyScoreboardEnabled() && this.isInLobbyWorld(player)) {
                this.scoreboardManager.applyLobbyScoreboard(player);
            }
        }, 4);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        // Remove scoreboard
        this.scoreboardManager.clearLobbyScoreboard(player);

        // Reset creation mode if active
        if (GameSetupHelper.isInCreationMode(player)) {
            GameSetupHelper.resetCreation(player);
        }

        // Save and remove user
        this.userManager.getUser(uuid).thenAccept(user -> {
            if (user != null) {
                this.userManager.saveUserToStorage(uuid);
                this.userManager.removeUserFromCache(uuid);
            }
        });
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();

        // Apply lobby effects when joining lobby
        if (this.isLobbySettingsEnabled() && this.isInLobbyWorld(player)) {
            this.applyJoinEffects(player);
        }

        // Reset creation mode if leaving setup world
        if (GameSetupHelper.isInCreationMode(player)) {
            Game game = GameSetupHelper.getGameFromCreationMode(player);
            if (game != null && !player.getWorld().getName().equals(game.getWorldName())) {
                GameSetupHelper.resetCreation(player);
            }
        }

        // Handle scoreboard
        if (this.scoreboardManager.isLobbyScoreboardEnabled()) {
            if (this.isInLobbyWorld(player)) {
                this.scoreboardManager.applyLobbyScoreboard(player);
            } else {
                this.scoreboardManager.clearLobbyScoreboard(player);
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!this.isLobbySettingsEnabled() || !this.isInLobbyWorld(player) ||
                PermissionHelper.hasSettingBypassPermission(player, "allow-hunger")) {
            return;
        }

        String allowHungerPath = SETTINGS_PATH + "allow-hunger";
        if (this.lobbyConfig.ensureBoolean(allowHungerPath)) {
            if (!this.lobbyConfig.getBoolean(allowHungerPath)) {
                event.setCancelled(true);
            }
        } else {
            this.plugin.getLogger().warning("Invalid value for " + allowHungerPath + " in lobby.yml!");
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();

        if (!this.isLobbySettingsEnabled() || !this.isInLobbyWorld(player) ||
                PermissionHelper.hasSettingBypassPermission(player, "allow-item-drop")) {
            return;
        }

        String allowDropPath = SETTINGS_PATH + "allow-item-drop";
        if (this.lobbyConfig.ensureBoolean(allowDropPath)) {
            if (!this.lobbyConfig.getBoolean(allowDropPath)) {
                event.setCancelled(true);
            }
        } else {
            this.plugin.getLogger().warning("Invalid value for " + allowDropPath + " in lobby.yml!");
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!this.isLobbySettingsEnabled() || !this.isInLobbyWorld(player)) {
            return;
        }

        String allowDamagePath = SETTINGS_PATH + "allow-player-damage";
        if (this.lobbyConfig.ensureBoolean(allowDamagePath)) {
            if (!this.lobbyConfig.getBoolean(allowDamagePath)) {
                event.setCancelled(true);
            }
        } else {
            this.plugin.getLogger().warning("Invalid value for " + allowDamagePath + " in lobby.yml!");
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (this.isLobbySettingsEnabled() && this.isLobbyWorld(event.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();

        if (this.isLobbySettingsEnabled() && this.isInLobbyWorld(player) &&
                !PermissionHelper.hasSettingBypassPermission(player, "allow-block-breaking")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        final Player player = event.getPlayer();

        if (this.isLobbySettingsEnabled() && this.isInLobbyWorld(player) &&
                !PermissionHelper.hasSettingBypassPermission(player, "allow-block-placing")) {
            event.setCancelled(true);
        }
    }

    /**
     * Applies join effects to a player.
     *
     * @param player Player to apply effects to
     */
    private void applyJoinEffects(@NotNull Player player) {
        Logger logger = this.plugin.getLogger();

        // Teleport to lobby spawn
        String lobbyPath = "environment.lobby";
        Location location = this.lobbyConfig.getLocation(lobbyPath, false);
        if (location == null) {
            logger.warning("Lobby location at " + lobbyPath + " is not set or invalid!");
        } else {
            player.teleport(location);
        }

        // Set game mode
        String gameModePath = SETTINGS_PATH + "on-join.set-gamemode";
        try {
            GameMode gameMode = GameMode.valueOf(this.lobbyConfig.getString(gameModePath).toUpperCase());
            player.setGameMode(gameMode);
        } catch (IllegalArgumentException exception) {
            logger.severe("Invalid Game Mode at " + gameModePath + " in lobby.yml!");
        }

        // Set health
        String healthPath = SETTINGS_PATH + "on-join.set-health";
        if (this.lobbyConfig.ensureInt(healthPath)) {
            int health = this.lobbyConfig.getInt(healthPath);
            player.setHealthScale(health);
            player.setHealth(health);
        } else {
            logger.warning("Invalid health value at " + healthPath + " in lobby.yml!");
        }

        // Set food level
        String foodLevelPath = SETTINGS_PATH + "on-join.set-food";
        if (this.lobbyConfig.ensureInt(foodLevelPath)) {
            player.setFoodLevel(this.lobbyConfig.getInt(foodLevelPath));
        } else {
            logger.warning("Invalid food level at " + foodLevelPath + " in lobby.yml!");
        }

        // Clear inventory
        String clearInventoryPath = SETTINGS_PATH + "on-join.clear-inventory";
        if (this.lobbyConfig.ensureBoolean(clearInventoryPath)) {
            if (this.lobbyConfig.getBoolean(clearInventoryPath)) {
                player.getInventory().clear();
            }
        } else {
            logger.warning("Invalid clear inventory value at " + clearInventoryPath + " in lobby.yml!");
        }
    }

    /**
     * Checks if player is in lobby world.
     *
     * @param player Player to check
     * @return true if in lobby world
     */
    private boolean isInLobbyWorld(@NotNull Player player) {
        return this.isLobbyWorld(player.getWorld());
    }

    /**
     * Checks if world is the lobby world.
     *
     * @param world World to check
     * @return true if lobby world
     */
    private boolean isLobbyWorld(@NotNull World world) {
        Location lobbyLocation = this.lobbyConfig.getLocation("environment.lobby", false);
        if (lobbyLocation == null) {
            return false;
        }

        World targetWorld = lobbyLocation.getWorld();
        return world.equals(targetWorld);
    }

    /**
     * Checks if lobby settings are enabled.
     *
     * @return true if enabled
     */
    private boolean isLobbySettingsEnabled() {
        if (!this.lobbyConfig.ensureBoolean(SETTINGS_PATH + "enable-settings")) {
            return false;
        }

        return this.lobbyConfig.getBoolean(SETTINGS_PATH + "enable-settings");
    }
}