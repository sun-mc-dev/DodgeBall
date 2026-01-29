package me.sunmc.dodgeball.api;

import me.sunmc.dodgeball.game.Game;
import me.sunmc.dodgeball.storage.user.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main API interface for Dodgeball plugin.
 * Other plugins can use this to interact with Dodgeball.
 *
 * <p>To get the API instance:
 * <pre>{@code
 * DodgeballPlugin plugin = (DodgeballPlugin) Bukkit.getPluginManager().getPlugin("Dodgeball");
 * DodgeballAPI api = plugin.getAPI();
 * }</pre>
 */
public interface DodgeballAPI {

    /**
     * Gets the API version.
     *
     * @return API version string
     */
    @NotNull
    String getAPIVersion();

    /**
     * Gets a game by its ID.
     *
     * @param gameId The game ID
     * @return The game or null if not found
     */
    @Nullable
    Game getGame(@NotNull String gameId);

    /**
     * Gets the game a player is currently in.
     *
     * @param player The player
     * @return The game or null if player is not in a game
     */
    @Nullable
    Game getPlayerGame(@NotNull Player player);

    /**
     * Gets all registered games.
     *
     * @return Map of game ID to Game
     */
    @NotNull
    Map<String, Game> getGames();

    /**
     * Gets a user by UUID.
     * This method is async and returns a CompletableFuture.
     *
     * @param uuid Player UUID
     * @return CompletableFuture with User or null
     */
    @NotNull
    CompletableFuture<User> getUser(@NotNull UUID uuid);

    /**
     * Gets a cached user by UUID.
     * Warning: This returns null if user is not cached.
     * Use {@link #getUser(UUID)} for safe access.
     *
     * @param uuid Player UUID
     * @return User or null if not cached
     */
    @Nullable
    User getCachedUser(@NotNull UUID uuid);

    /**
     * Checks if a player is in a game.
     *
     * @param player The player
     * @return true if player is in a game
     */
    boolean isInGame(@NotNull Player player);

    /**
     * Adds a player to the first available game.
     *
     * @param player The player to add
     * @return true if successfully added to a game
     */
    boolean joinGame(@NotNull Player player);

    /**
     * Removes a player from their current game.
     *
     * @param player The player to remove
     * @return true if successfully removed
     */
    boolean leaveGame(@NotNull Player player);
}