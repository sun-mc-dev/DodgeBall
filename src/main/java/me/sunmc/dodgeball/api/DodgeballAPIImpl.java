package me.sunmc.dodgeball.api;

import me.sunmc.dodgeball.DodgeballPlugin;
import me.sunmc.dodgeball.game.Game;
import me.sunmc.dodgeball.game.GameHelper;
import me.sunmc.dodgeball.storage.user.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the Dodgeball API.
 */
public class DodgeballAPIImpl implements DodgeballAPI {

    private static final String API_VERSION = "2.0.0";
    private final DodgeballPlugin plugin;

    public DodgeballAPIImpl(@NotNull DodgeballPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAPIVersion() {
        return API_VERSION;
    }

    @Override
    public @Nullable Game getGame(@NotNull String gameId) {
        return plugin.getGameById(gameId);
    }

    @Override
    public @Nullable Game getPlayerGame(@NotNull Player player) {
        return GameHelper.getGameFromPlayer(player);
    }

    @Override
    public @NotNull Map<String, Game> getGames() {
        return Map.copyOf(plugin.getGames());
    }

    @Override
    public @NotNull CompletableFuture<User> getUser(@NotNull UUID uuid) {
        return plugin.getUserStorage().getUser(uuid);
    }

    @Override
    public @Nullable User getCachedUser(@NotNull UUID uuid) {
        return plugin.getUserStorage().getCachedUser(uuid);
    }

    @Override
    public boolean isInGame(@NotNull Player player) {
        return GameHelper.getGameFromPlayer(player) != null;
    }

    @Override
    public boolean joinGame(@NotNull Player player) {
        // Find first available game
        for (Game game : plugin.getGames().values()) {
            if (game.getGameState().isWaiting() && game.isEnabled()) {
                game.sendPlayer(player);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean leaveGame(@NotNull Player player) {
        Game game = GameHelper.getGameFromPlayer(player);
        if (game != null) {
            game.removePlayer(player);
            return true;
        }
        return false;
    }
}