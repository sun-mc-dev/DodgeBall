package me.sunmc.dodgeball.commands.subcommands;

import me.sunmc.dodgeball.DodgeballPlugin;
import me.sunmc.dodgeball.commands.core.AbstractPlayerSubcommand;
import me.sunmc.dodgeball.game.Game;
import me.sunmc.dodgeball.game.setup.GameSetupHelper;
import me.sunmc.dodgeball.managers.world.WorldManager;
import me.sunmc.dodgeball.storage.GameFileStorage;
import me.sunmc.dodgeball.utility.DefaultSound;
import me.sunmc.dodgeball.utility.PermissionHelper;
import me.sunmc.dodgeball.utility.SoundHelper;
import me.sunmc.dodgeball.utility.messaging.LibColor;
import me.sunmc.dodgeball.utility.messaging.lang.MessageHelper;
import me.sunmc.dodgeball.utility.messaging.lang.MsgReplace;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Initiates the game setup process.
 */
public class SetupGameSubcommand extends AbstractPlayerSubcommand {

    private final @NonNull DodgeballPlugin plugin;
    private final @NonNull WorldManager worldManager;
    private final @NonNull GameFileStorage config;

    public SetupGameSubcommand(@NonNull DodgeballPlugin plugin) {
        super("dbadmin_setupgame");
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        this.config = plugin.getPluginConfig();
    }

    @Override
    public void onPlayerSubcommand(@NonNull Player player, @NonNull String[] args) {
        // Checks if player has permission
        if (!PermissionHelper.hasAdminCommandPermission(player, "setupgame")) {
            MessageHelper.sendMessage(player, "command.dbadmin.no-permission", new MsgReplace("permission", "dbadmin.command.setupgame"));
            return;
        }

        // Put player into setup mode
        if (!GameSetupHelper.setPlayerIntoSetupMode(player)) {
            return;
        }

        player.sendMessage(LibColor.colorMessage("&7Starting arena creation..."));
        GameSetupHelper.savePlayer(player);

        // Set up a new modified world
        int currentGameIndex = this.config.getInt("current-game-index");
        World world = this.worldManager.setupModifiedWorld("dodgeball_arena_" + currentGameIndex);

        // Increment the game index
        this.config.set("current-game-index", currentGameIndex + 1);

        if (world == null) {
            player.sendMessage(LibColor.colorMessage("&c&lERROR! &cSomething went wrong when creating the world, check the console!"));
            return;
        }

        // Create new game
        Game game = new Game(this.plugin, "dodgeball_" + currentGameIndex, world.getName());
        this.plugin.addGame(game);
        GameSetupHelper.setGameInCreationMode(player, game);

        // Notify player of creating
        SoundHelper.playDefaultSound(player, DefaultSound.SUCCESS_LIGHT);
        player.sendMessage(LibColor.colorMessage("&a&lARENA CREATED! &aA new dogeball arena was put into creation, teleporting you to it!"));

        // Teleport and enable flight
        Location location = new Location(world, 0, 100, 0);

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            player.teleport(location);
            player.setGameMode(GameMode.CREATIVE);
            player.setFlying(true);
            player.setAllowFlight(true);
        }, 5);
    }
}











