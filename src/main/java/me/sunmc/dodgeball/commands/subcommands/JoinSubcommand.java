package me.sunmc.dodgeball.commands.subcommands;

import me.sunmc.dodgeball.DodgeballPlugin;
import me.sunmc.dodgeball.commands.core.AbstractPlayerSubcommand;
import me.sunmc.dodgeball.game.Game;
import me.sunmc.dodgeball.game.GameHelper;
import me.sunmc.dodgeball.utility.PermissionHelper;
import me.sunmc.dodgeball.utility.messaging.lang.MessageHelper;
import me.sunmc.dodgeball.utility.messaging.lang.MsgReplace;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;

/**
 * Joins the first available Dodgeball game.
 */
public class JoinSubcommand extends AbstractPlayerSubcommand {

    private final @NonNull Map<String, Game> games;

    public JoinSubcommand(@NonNull DodgeballPlugin plugin) {
        super("dodgeball_join");
        this.games = plugin.getGames();
    }

    @Override
    public void onPlayerSubcommand(@NonNull Player player, @NonNull String[] args) {
        // Check if player has permission
        if (!PermissionHelper.hasPermission(player, "join")) {
            MessageHelper.sendMessage(player, "command.dodgeball.no-permission", new MsgReplace("permission", "dodgeball.command.join"));
            return;
        }

        // Cannot join a game if you are already in one
        Game game = GameHelper.getGameFromPlayer(player);
        if (game != null) {
            MessageHelper.sendMessage(player, "join-game.already-in-game");
            return;
        }

        // Loop through games and find an available one
        for (Game targetGame : this.games.values()) {
            if (targetGame.getGameState().isWaiting()) {
                MessageHelper.sendMessage(player, "join-game.game-found");
                targetGame.sendPlayer(player);
                return;
            }
        }

        // No game found
        MessageHelper.sendMessage(player, "join-game.no-games-available");
    }
}