package me.sunmc.dodgeball.tickhandler.runnables;

import me.sunmc.dodgeball.DodgeballPlugin;
import me.sunmc.dodgeball.game.Game;
import me.sunmc.dodgeball.managers.ScoreboardManager;
import me.sunmc.dodgeball.storage.GameFileStorage;
import me.sunmc.dodgeball.tickhandler.AbstractGameRunnable;
import me.sunmc.dodgeball.utility.autoregistry.AutoRegister;
import me.sunmc.dodgeball.utility.autoregistry.AutoRegistry;
import org.bukkit.Location;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Runnable to update all scoreboard each 10 ticks.
 */
@AutoRegister(type = AutoRegistry.Type.RUNNABLE)
public class ScoreboardRunnable extends AbstractGameRunnable {

    private final @NonNull DodgeballPlugin plugin;

    public ScoreboardRunnable(@NonNull DodgeballPlugin plugin) {
        super("scoreboard", 10);
        this.plugin = plugin;
    }

    @Override
    public void run() {
        ScoreboardManager scoreboardManager = this.plugin.getScoreboardManager();
        GameFileStorage configuration = this.plugin.getLobbyConfig();

        // Update game scoreboard
        for (Game game : this.plugin.getGames().values()) {
            if (!game.getPlayers().isEmpty()) {
                game.updateScoreboard();
            }
        }

        // Update lobby scoreboard
        Location location = configuration.getLocation("environment.lobby", false);
        if (location != null) {
            World world = location.getWorld();

            // Update the lobby scoreboard lines every tick if a player is in the lobby
            if (world != null && !world.getPlayers().isEmpty()) {
                scoreboardManager.updateLobbyScoreboards();
            }
        }
    }
}








