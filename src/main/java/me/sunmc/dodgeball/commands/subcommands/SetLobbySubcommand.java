package me.sunmc.dodgeball.commands.subcommands;

import me.sunmc.dodgeball.DodgeballPlugin;
import me.sunmc.dodgeball.commands.core.AbstractPlayerSubcommand;
import me.sunmc.dodgeball.managers.ScoreboardManager;
import me.sunmc.dodgeball.storage.GameFileStorage;
import me.sunmc.dodgeball.utility.DefaultSound;
import me.sunmc.dodgeball.utility.PermissionHelper;
import me.sunmc.dodgeball.utility.SoundHelper;
import me.sunmc.dodgeball.utility.location.LocationHelper;
import me.sunmc.dodgeball.utility.messaging.LibColor;
import me.sunmc.dodgeball.utility.messaging.lang.MessageHelper;
import me.sunmc.dodgeball.utility.messaging.lang.MsgReplace;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Sets the main server game lobby.
 */
public class SetLobbySubcommand extends AbstractPlayerSubcommand {

    private final @NonNull DodgeballPlugin plugin;
    private final @NonNull GameFileStorage lobbyConfig;

    public SetLobbySubcommand(@NonNull DodgeballPlugin plugin) {
        super("dbadmin_setlobby");
        this.plugin = plugin;
        this.lobbyConfig = plugin.getLobbyConfig();
    }

    @Override
    public void onPlayerSubcommand(@NonNull Player player, @NonNull String[] args) {
        // Checks if player has permission
        if (!PermissionHelper.hasAdminCommandPermission(player, "setlobby")) {
            MessageHelper.sendMessage(player, "command.dbadmin.no-permission", new MsgReplace("permission", "dbadmin.command.setlobby"));
            return;
        }

        // Get the location and serialize it to a string
        final Location location = player.getLocation();
        String serializedLocation = LocationHelper.writeLocation(location);

        // Set the server lobby to the player's current location
        this.lobbyConfig.setAsync("environment.lobby", serializedLocation).thenAccept((v) -> {
            ScoreboardManager manager = this.plugin.getScoreboardManager();

            // Apply to players in the world
            World world = location.getWorld();
            if (world != null) {
                for (Player target : world.getPlayers()) {
                    manager.applyLobbyScoreboard(target);
                }
            }

            SoundHelper.playDefaultSound(player, DefaultSound.SUCCESS_LIGHT);
            String friendlyLobbyLocation = LocationHelper.friendlyLocationText(location, ChatColor.GREEN, ChatColor.GOLD);
            player.sendMessage(LibColor.colorMessage("&a&lLOBBY SET! &aThe lobby location was set to " + friendlyLobbyLocation + "."));
        });
    }
}











