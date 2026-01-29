package me.sunmc.dodgeball.commands;

import me.sunmc.dodgeball.DodgeballPlugin;
import me.sunmc.dodgeball.commands.core.AbstractPlayerCommand;
import me.sunmc.dodgeball.commands.core.TabCompleteData;
import me.sunmc.dodgeball.commands.core.TabOption;
import me.sunmc.dodgeball.commands.subcommands.*;
import me.sunmc.dodgeball.utility.PermissionHelper;
import me.sunmc.dodgeball.utility.autoregistry.AutoRegister;
import me.sunmc.dodgeball.utility.autoregistry.AutoRegistry;
import me.sunmc.dodgeball.utility.messaging.lang.MessageHelper;
import me.sunmc.dodgeball.utility.messaging.lang.MsgReplace;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * /dbadmin command. The administration command to set up new games.
 */
@AutoRegister(type = AutoRegistry.Type.COMMAND)
public class DodgeballAdminCommand extends AbstractPlayerCommand {

    public DodgeballAdminCommand(@NonNull DodgeballPlugin plugin) {
        super("dbadmin", new TabCompleteData(
                        new TabOption(1,
                                "setlobby", "setupgame", "setwaitinglobby", "createteam", "deleteteam", "playablearea", "completesetup")
                ),
                new SetLobbySubcommand(plugin),
                new SetupGameSubcommand(plugin),
                new CreateTeamSubCommand(plugin),
                new DeleteTeamSubcommand(plugin),
                new SetWaitingLobbySubcommand(plugin),
                new PlayableAreaSubcommand(plugin),
                new CreateTeamSubCommand(plugin),
                new CompleteSetupSubcommand(plugin)
        );
    }

    @Override
    public void onPlayerCommand(@NonNull Player player, @NonNull String[] args) {
        // Check if the player has the base permission to use the command
        if (!PermissionHelper.hasAdminCommandPermission(player, "use")) {
            MessageHelper.sendMessage(player, "command.dbadmin.no-permission", new MsgReplace("permission", "dbadmin.command.use"));
            return;
        }

        // Check if the player did not provide any subcommand
        if (args.length == 0) {
            MessageHelper.sendMessage(player, "command.dbadmin.invalid-argument");
            return;
        }

        String argument = args[0].toLowerCase();
        switch (argument) {
            case "setlobby" -> this.callSubcommand(player, "dbadmin_setlobby", args);
            case "setupgame" -> this.callSubcommand(player, "dbadmin_setupgame", args);
            case "setwaitinglobby" -> this.callSubcommand(player, "dbadmin_setwaitinglobby", args);
            case "createteam" -> this.callSubcommand(player, "dbadmin_createteam", args);
            case "deleteteam" -> this.callSubcommand(player, "dbadmin_deleteteam", args);
            case "playablearea" -> this.callSubcommand(player, "dbadmin_playablearea", args);
            case "completesetup" -> this.callSubcommand(player, "dbadmin_completesetup", args);
            default -> MessageHelper.sendMessage(player, "command.dbadmin.invalid-argument");
        }
    }
}