package me.sunmc.dodgeball.tickhandler.runnables;

import me.sunmc.dodgeball.DodgeballPlugin;
import me.sunmc.dodgeball.game.Game;
import me.sunmc.dodgeball.game.GamePlayer;
import me.sunmc.dodgeball.game.enums.GameState;
import me.sunmc.dodgeball.tickhandler.AbstractGameRunnable;
import me.sunmc.dodgeball.utility.DefaultSound;
import me.sunmc.dodgeball.utility.SoundHelper;
import me.sunmc.dodgeball.utility.TimeDisplayHelper;
import me.sunmc.dodgeball.utility.autoregistry.AutoRegister;
import me.sunmc.dodgeball.utility.autoregistry.AutoRegistry;
import me.sunmc.dodgeball.utility.messaging.LibTitle;
import me.sunmc.dodgeball.utility.messaging.lang.MessageHelper;
import me.sunmc.dodgeball.utility.messaging.lang.MsgReplace;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Runnable to manager the waiting timer when waiting for
 * a game to start.
 */
@AutoRegister(type = AutoRegistry.Type.RUNNABLE)
public class TimerRunnable extends AbstractGameRunnable {

    private final @NonNull DodgeballPlugin plugin;

    public TimerRunnable(@NonNull DodgeballPlugin plugin) {
        super("timer", 20);
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Game game : this.plugin.getGames().values()) {
            final GameState gameState = game.getGameState();

            // Handle the waiting timer before the game starts
            if (gameState == GameState.WAITING) {
                int countdown = game.getWaitingCountdown();

                // Waiting countdown is done, start the game
                if (countdown == 0) {
                    game.setDelayedGameState(GameState.ACTIVE, 2);
                    continue;
                }

                MsgReplace replace = new MsgReplace("timer", TimeDisplayHelper.secondsToMinSecWithoutText(countdown));
                for (GamePlayer gamePlayer : game.getPlayers()) {
                    Player player = gamePlayer.toPlayer();
                    if (player == null) {
                        continue;
                    }

                    if (countdown <= 10) {
                        LibTitle.sendTitle(player, 10, 20, 10, "&6" + TimeDisplayHelper.getTimeIcon(countdown), "");
                    }

                    SoundHelper.playDefaultSound(player, DefaultSound.CLICK);
                    MessageHelper.sendMessage(player, "game.starting-in", replace);
                }

                game.decreaseWaitingCountdown();
            }
        }
    }
}









