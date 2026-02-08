package me.sunmc.dodgeball.api;

import me.sunmc.dodgeball.arena.Arena;
import me.sunmc.dodgeball.arena.ArenaState;
import me.sunmc.dodgeball.game.GameMode;
import me.sunmc.dodgeball.player.DodgeBallPlayer;
import me.sunmc.dodgeball.stats.PlayerStats;
import me.sunmc.dodgeball.team.Team;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for DodgeBall Plugin
 *
 * This interface provides comprehensive access to all DodgeBall features
 * for external plugins and developers.
 *
 * Thread-Safety: All methods are thread-safe unless otherwise noted.
 * Async Operations: Methods returning CompletableFuture are async.
 */
public interface DodgeBallAPI {


    /**
     * Gets an arena by its identifier
     *
     * @param arenaId Arena identifier
     * @return Optional containing the arena if found
     */
    @NonNull Optional<Arena> getArena(@NonNull String arenaId);

    /**
     * Gets all registered arenas
     *
     * @return Immutable list of all arenas
     */
    @NonNull List<Arena> getArenas();

    /**
     * Gets all arenas in a specific state
     *
     * @param state Arena state to filter by
     * @return List of arenas in the specified state
     */
    @NonNull List<Arena> getArenasByState(@NonNull ArenaState state);

    /**
     * Creates a new arena asynchronously
     *
     * @param arenaId Arena identifier
     * @param displayName Arena display name
     * @param minPlayers Minimum players required
     * @param maxPlayers Maximum players allowed
     * @param gameMode Game mode for this arena
     * @return CompletableFuture completing with the created arena
     */
    @NonNull CompletableFuture<Arena> createArena(
            @NonNull String arenaId,
            @NonNull String displayName,
            int minPlayers,
            int maxPlayers,
            @NonNull GameMode gameMode
    );

    /**
     * Deletes an arena asynchronously
     *
     * @param arenaId Arena identifier
     * @return CompletableFuture completing with success status
     */
    @NonNull CompletableFuture<Boolean> deleteArena(@NonNull String arenaId);

    /**
     * Sets a location for an arena
     *
     * @param arenaId Arena identifier
     * @param locationType Location type (LOBBY, TEAM1_SPAWN, TEAM2_SPAWN, etc.)
     * @param location Location to set
     * @return True if successful
     */
    boolean setArenaLocation(
            @NonNull String arenaId,
            @NonNull String locationType,
            @NonNull Location location
    );


    /**
     * Gets a DodgeBall player wrapper
     *
     * @param player Bukkit player
     * @return DodgeBall player wrapper
     */
    @NonNull DodgeBallPlayer getDodgeBallPlayer(@NonNull Player player);

    /**
     * Gets a DodgeBall player by UUID
     *
     * @param uuid Player UUID
     * @return Optional containing the player if online
     */
    @NonNull Optional<DodgeBallPlayer> getDodgeBallPlayer(@NonNull UUID uuid);

    /**
     * Checks if a player is in a game
     *
     * @param player Player to check
     * @return True if player is in a game
     */
    boolean isInGame(@NonNull Player player);

    /**
     * Gets the arena a player is currently in
     *
     * @param player Player to check
     * @return Optional containing the arena if player is in one
     */
    @NonNull Optional<Arena> getPlayerArena(@NonNull Player player);

    /**
     * Makes a player join an arena
     *
     * @param player Player to join
     * @param arenaId Arena identifier
     * @return CompletableFuture completing with success status
     */
    @NonNull CompletableFuture<Boolean> joinArena(
            @NonNull Player player,
            @NonNull String arenaId
    );

    /**
     * Makes a player leave their current arena
     *
     * @param player Player to remove
     * @return True if player was removed from an arena
     */
    boolean leaveArena(@NonNull Player player);


    /**
     * Starts a game in an arena
     *
     * @param arenaId Arena identifier
     * @return CompletableFuture completing with success status
     */
    @NonNull CompletableFuture<Boolean> startGame(@NonNull String arenaId);

    /**
     * Stops a game in an arena
     *
     * @param arenaId Arena identifier
     * @return CompletableFuture completing with success status
     */
    @NonNull CompletableFuture<Boolean> stopGame(@NonNull String arenaId);

    /**
     * Forces a game to end
     *
     * @param arenaId Arena identifier
     * @param winningTeam Team that won (null for draw)
     * @return True if game was ended
     */
    boolean forceEndGame(@NonNull String arenaId, @Nullable Team winningTeam);


    /**
     * Gets a player's team in their current arena
     *
     * @param player Player to check
     * @return Optional containing the team if player is in a game
     */
    @NonNull Optional<Team> getPlayerTeam(@NonNull Player player);

    /**
     * Switches a player to a different team
     *
     * @param player Player to switch
     * @param team Target team
     * @return True if switch was successful
     */
    boolean switchTeam(@NonNull Player player, @NonNull Team team);


    /**
     * Gets player statistics
     *
     * @param uuid Player UUID
     * @return CompletableFuture completing with player stats
     */
    @NonNull CompletableFuture<PlayerStats> getPlayerStats(@NonNull UUID uuid);

    /**
     * Gets top players by a specific stat
     *
     * @param statType Stat type (KILLS, WINS, CATCHES, etc.)
     * @param limit Number of players to return
     * @return CompletableFuture completing with list of top players
     */
    @NonNull CompletableFuture<List<PlayerStats>> getTopPlayers(
            @NonNull String statType,
            int limit
    );

    /**
     * Resets a player's statistics
     *
     * @param uuid Player UUID
     * @return CompletableFuture completing with success status
     */
    @NonNull CompletableFuture<Boolean> resetPlayerStats(@NonNull UUID uuid);


    /**
     * Reloads all plugin configurations
     *
     * @return CompletableFuture completing when reload is done
     */
    @NonNull CompletableFuture<Void> reloadConfig();

    /**
     * Gets a configuration value
     *
     * @param path Configuration path
     * @return Configuration value as Object
     */
    @Nullable Object getConfigValue(@NonNull String path);

    /**
     * Sets a configuration value
     *
     * @param path Configuration path
     * @param value Value to set
     * @return True if value was set successfully
     */
    boolean setConfigValue(@NonNull String path, @NonNull Object value);


    /**
     * Registers an event listener for DodgeBall events
     *
     * @param listener Event listener instance
     */
    void registerEventListener(@NonNull Object listener);

    /**
     * Unregisters an event listener
     *
     * @param listener Event listener instance
     */
    void unregisterEventListener(@NonNull Object listener);

    /**
     * Gets the API version
     *
     * @return API version string
     */
    @NonNull String getAPIVersion();

    /**
     * Checks if the plugin is in debug mode
     *
     * @return True if debug mode is enabled
     */
    boolean isDebugMode();

    /**
     * Enables or disables debug mode
     *
     * @param debug Debug mode state
     */
    void setDebugMode(boolean debug);
}