package me.sunmc.dodgeball.component;


import me.sunmc.dodgeball.DodgeBall;
import me.sunmc.dodgeball.arena.Arena;
import me.sunmc.dodgeball.game.Game;
import me.sunmc.tools.component.Component;
import me.sunmc.tools.component.DependencyComponent;
import me.sunmc.tools.registry.AutoRegister;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages game instances - FULLY IMPLEMENTED
 */
@AutoRegister(Component.class)
@DependencyComponent({ArenaManager.class, BallManager.class})
public class GameManager implements Component {

    private final @NonNull DodgeBall plugin;
    private final @NonNull Map<Arena, Game> activeGames;

    public GameManager(@NonNull DodgeBall plugin) {
        this.plugin = plugin;
        this.activeGames = new ConcurrentHashMap<>();
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("Game manager enabled");
    }

    @Override
    public void onDisable() {
        activeGames.values().forEach(me.sunmc.dodgeball.game.Game::end);
        activeGames.clear();
    }

    public void startGame(@NonNull Arena arena) {
        if (activeGames.containsKey(arena)) {
            return;
        }

        Game game = new Game(arena, arena.getGameMode(), plugin);
        game.start();
        activeGames.put(arena, game);
    }

    public void endGame(@NonNull Arena arena) {
        Game game = activeGames.remove(arena);
        if (game != null) {
            game.end();
        }
    }

    public @Nullable Game getGame(@NonNull Arena arena) {
        return activeGames.get(arena);
    }
}