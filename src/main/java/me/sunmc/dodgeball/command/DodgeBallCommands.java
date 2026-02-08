package me.sunmc.dodgeball.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.sunmc.dodgeball.DodgeBall;
import me.sunmc.dodgeball.arena.Arena;
import me.sunmc.dodgeball.component.ArenaManager;
import me.sunmc.dodgeball.component.GameManager;
import me.sunmc.dodgeball.game.GameMode;
import me.sunmc.dodgeball.menu.ArenaListMenu;
import me.sunmc.dodgeball.menu.StatsMenu;
import me.sunmc.tools.command.CommandFactory;
import me.sunmc.tools.configuration.ConfigurationProvider;
import me.sunmc.tools.registry.AutoRegister;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Objects;

/**
 * Complete Command System - NO TODOs
 */
@AutoRegister(CommandFactory.class)
public class DodgeBallCommands implements CommandFactory {

    private final @NonNull DodgeBall plugin;

    public DodgeBallCommands(@NonNull DodgeBall plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NonNull List<CommandAPICommand> buildMultipleCommands() {
        return List.of(
                buildMainCommand(),
                buildAdminCommand()
        );
    }

    private @NonNull CommandAPICommand buildMainCommand() {
        return new CommandAPICommand("dodgeball")
                .withAliases("db", "dodge")
                .withPermission("dodgeball.use")
                .withSubcommand(buildJoinCommand())
                .withSubcommand(buildLeaveCommand())
                .withSubcommand(buildListCommand())
                .withSubcommand(buildStatsCommand())
                .executesPlayer((PlayerCommandExecutor) (player, args) -> sendHelpMessage(player));
    }

    private @NonNull CommandAPICommand buildAdminCommand() {
        return new CommandAPICommand("dodgeballadmin")
                .withAliases("dba", "dbadmin")
                .withPermission("dodgeball.admin")
                .withSubcommand(buildCreateCommand())
                .withSubcommand(buildDeleteCommand())
                .withSubcommand(buildSetupCommand())
                .withSubcommand(buildStartCommand())
                .withSubcommand(buildStopCommand())
                .withSubcommand(buildReloadCommand())
                .withSubcommand(buildInfoCommand())
                .executesPlayer((PlayerCommandExecutor) (player, args) -> sendAdminHelpMessage(player));
    }


    private @NonNull CommandAPICommand buildJoinCommand() {
        return new CommandAPICommand("join")
                .withArguments(new StringArgument("arena")
                        .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                plugin.getComponent(ArenaManager.class).getArenas().stream()
                                        .filter(a -> a.getState() == me.sunmc.dodgeball.arena.ArenaState.WAITING ||
                                                a.getState() == me.sunmc.dodgeball.arena.ArenaState.STARTING)
                                        .map(Arena::getArenaId)
                                        .toArray(String[]::new)
                        )))
                .executesPlayer((player, args) -> {
                    String arenaId = (String) args.get("arena");
                    ArenaManager manager = plugin.getComponent(ArenaManager.class);

                    Arena arena = manager.getArena(arenaId);
                    if (arena == null) {
                        player.sendMessage(msg("arena-not-found"));
                        return;
                    }

                    if (!arena.isSetup()) {
                        player.sendMessage(Component.text("§cThis arena is not fully set up!", NamedTextColor.RED));
                        return;
                    }

                    if (arena.isFull()) {
                        player.sendMessage(msg("arena-full"));
                        return;
                    }

                    if (manager.addPlayer(player, arena)) {
                        player.sendMessage(msg("join-success")
                                .replaceText(builder -> builder.matchLiteral("{arena}")
                                        .replacement(arena.getDisplayName())));
                        player.playSound(player.getLocation(),
                                org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    } else {
                        player.sendMessage(Component.text("§cFailed to join arena!", NamedTextColor.RED));
                    }
                });
    }

    private @NonNull CommandAPICommand buildLeaveCommand() {
        return new CommandAPICommand("leave")
                .executesPlayer((player, args) -> {
                    ArenaManager manager = plugin.getComponent(ArenaManager.class);
                    Arena arena = manager.getPlayerArena(player);

                    if (arena == null) {
                        player.sendMessage(msg("not-in-arena"));
                        return;
                    }

                    manager.removePlayer(player.getUniqueId());
                    player.sendMessage(msg("leave-success"));
                    player.playSound(player.getLocation(),
                            org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                });
    }

    private @NonNull CommandAPICommand buildListCommand() {
        return new CommandAPICommand("list")
                .executesPlayer((player, args) -> {
                    new ArenaListMenu(player).open();
                });
    }

    private @NonNull CommandAPICommand buildStatsCommand() {
        return new CommandAPICommand("stats")
                .withOptionalArguments(new PlayerArgument("player"))
                .executesPlayer((player, args) -> {
                    Player target = args.getOptional("player")
                            .map(obj -> (Player) obj)
                            .orElse(player);

                    new StatsMenu(player, target).open();
                });
    }

    private @NonNull CommandAPICommand buildCreateCommand() {
        return new CommandAPICommand("create")
                .withArguments(
                        new StringArgument("id"),
                        new GreedyStringArgument("name"),
                        new IntegerArgument("min", 1, 50),
                        new IntegerArgument("max", 1, 50),
                        new StringArgument("mode")
                                .replaceSuggestions(ArgumentSuggestions.strings(
                                        "CLASSIC", "ELIMINATION", "INFECTION", "KING_OF_THE_HILL", "CAPTURE_THE_FLAG"))
                )
                .executesPlayer((player, args) -> {
                    String id = (String) args.get("id");
                    String name = (String) args.get("name");
                    int min = (int) args.get("min");
                    int max = (int) args.get("max");
                    String modeStr = (String) args.get("mode");

                    if (min > max) {
                        player.sendMessage(Component.text("§cMin players cannot be greater than max!",
                                NamedTextColor.RED));
                        return;
                    }

                    GameMode mode;
                    try {
                        mode = GameMode.valueOf(modeStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Component.text("§cInvalid game mode!", NamedTextColor.RED));
                        return;
                    }

                    ArenaManager manager = plugin.getComponent(ArenaManager.class);

                    if (manager.getArena(id) != null) {
                        player.sendMessage(Component.text("§cArena with that ID already exists!",
                                NamedTextColor.RED));
                        return;
                    }

                    manager.createArena(id, name, min, max, mode);
                    player.sendMessage(Component.text("§aCreated arena: §e" + name, NamedTextColor.GREEN));
                    player.sendMessage(Component.text("§7Use §e/dba setup " + id + " <type> §7to set locations",
                            NamedTextColor.GRAY));
                });
    }

    private @NonNull CommandAPICommand buildDeleteCommand() {
        return new CommandAPICommand("delete")
                .withArguments(new StringArgument("arena")
                        .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                plugin.getComponent(ArenaManager.class).getArenas().stream()
                                        .map(Arena::getArenaId)
                                        .toArray(String[]::new)
                        )))
                .executesPlayer((player, args) -> {
                    String arenaId = (String) args.get("arena");
                    ArenaManager manager = plugin.getComponent(ArenaManager.class);

                    if (manager.deleteArena(arenaId)) {
                        player.sendMessage(Component.text("§aDeleted arena: §e" + arenaId,
                                NamedTextColor.GREEN));
                    } else {
                        player.sendMessage(Component.text("§cArena not found!", NamedTextColor.RED));
                    }
                });
    }

    private @NonNull CommandAPICommand buildSetupCommand() {
        return new CommandAPICommand("setup")
                .withArguments(
                        new StringArgument("arena")
                                .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                        plugin.getComponent(ArenaManager.class).getArenas().stream()
                                                .map(Arena::getArenaId)
                                                .toArray(String[]::new)
                                )),
                        new StringArgument("type")
                                .replaceSuggestions(ArgumentSuggestions.strings(
                                        "LOBBY", "CENTER", "TEAM1_SPAWN", "TEAM2_SPAWN",
                                        "SPECTATOR", "MIN_BOUND", "MAX_BOUND"
                                ))
                )
                .executesPlayer((player, args) -> {
                    String arenaId = (String) args.get("arena");
                    String type = ((String) args.get("type")).toUpperCase();

                    ArenaManager manager = plugin.getComponent(ArenaManager.class);
                    Arena arena = manager.getArena(arenaId);

                    if (arena == null) {
                        player.sendMessage(Component.text("§cArena not found!", NamedTextColor.RED));
                        return;
                    }

                    Location loc = player.getLocation();
                    arena.setLocation(type, loc);

                    player.sendMessage(Component.text("§aSet §e" + type + " §afor arena §e" + arenaId,
                            NamedTextColor.GREEN));

                    if (arena.isSetup()) {
                        player.sendMessage(Component.text("§a✓ Arena is fully set up!", NamedTextColor.GREEN));
                    } else {
                        player.sendMessage(Component.text("§7Missing locations:", NamedTextColor.GRAY));
                        for (String locType : List.of("LOBBY", "CENTER", "TEAM1_SPAWN", "TEAM2_SPAWN",
                                "MIN_BOUND", "MAX_BOUND")) {
                            if (!arena.hasLocation(locType)) {
                                player.sendMessage(Component.text("  §c✗ " + locType, NamedTextColor.RED));
                            }
                        }
                    }
                });
    }

    private @NonNull CommandAPICommand buildStartCommand() {
        return new CommandAPICommand("start")
                .withArguments(new StringArgument("arena")
                        .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                plugin.getComponent(ArenaManager.class).getArenas().stream()
                                        .map(Arena::getArenaId)
                                        .toArray(String[]::new)
                        )))
                .executesPlayer((player, args) -> {
                    String arenaId = (String) args.get("arena");
                    ArenaManager arenaManager = plugin.getComponent(ArenaManager.class);
                    GameManager gameManager = plugin.getComponent(GameManager.class);

                    Arena arena = arenaManager.getArena(arenaId);
                    if (arena == null) {
                        player.sendMessage(Component.text("§cArena not found!", NamedTextColor.RED));
                        return;
                    }

                    if (!arena.isSetup()) {
                        player.sendMessage(Component.text("§cArena is not fully set up!", NamedTextColor.RED));
                        return;
                    }

                    if (arena.getPlayers().isEmpty()) {
                        player.sendMessage(Component.text("§cNo players in arena!", NamedTextColor.RED));
                        return;
                    }

                    gameManager.startGame(arena);
                    player.sendMessage(Component.text("§aForce-started game in arena: §e" + arenaId,
                            NamedTextColor.GREEN));
                });
    }

    private @NonNull CommandAPICommand buildStopCommand() {
        return new CommandAPICommand("stop")
                .withArguments(new StringArgument("arena")
                        .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                plugin.getComponent(ArenaManager.class).getArenas().stream()
                                        .filter(a -> a.getState() == me.sunmc.dodgeball.arena.ArenaState.IN_GAME)
                                        .map(Arena::getArenaId)
                                        .toArray(String[]::new)
                        )))
                .executesPlayer((player, args) -> {
                    String arenaId = (String) args.get("arena");
                    GameManager gameManager = plugin.getComponent(GameManager.class);
                    ArenaManager arenaManager = plugin.getComponent(ArenaManager.class);

                    Arena arena = arenaManager.getArena(Objects.requireNonNull(arenaId));
                    if (arena == null) {
                        player.sendMessage(Component.text("§cArena not found!", NamedTextColor.RED));
                        return;
                    }

                    gameManager.endGame(arena);
                    player.sendMessage(Component.text("§aStopped game in arena: §e" + arenaId,
                            NamedTextColor.GREEN));
                });
    }

    private @NonNull CommandAPICommand buildReloadCommand() {
        return new CommandAPICommand("reload")
                .executesPlayer((player, args) -> {
                    plugin.onReload();
                    player.sendMessage(Component.text("§aConfiguration reloaded!", NamedTextColor.GREEN));
                });
    }

    private @NonNull CommandAPICommand buildInfoCommand() {
        return new CommandAPICommand("info")
                .withArguments(new StringArgument("arena")
                        .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                plugin.getComponent(ArenaManager.class).getArenas().stream()
                                        .map(Arena::getArenaId)
                                        .toArray(String[]::new)
                        )))
                .executesPlayer((player, args) -> {
                    String arenaId = (String) args.get("arena");
                    ArenaManager manager = plugin.getComponent(ArenaManager.class);
                    Arena arena = manager.getArena(arenaId);

                    if (arena == null) {
                        player.sendMessage(Component.text("§cArena not found!", NamedTextColor.RED));
                        return;
                    }

                    player.sendMessage(Component.text("§6§l=== Arena Info: " + arena.getDisplayName() + " ==="));
                    player.sendMessage(Component.text("§7ID: §f" + arena.getArenaId()));
                    player.sendMessage(Component.text("§7State: §f" + arena.getState()));
                    player.sendMessage(Component.text("§7Mode: §f" + arena.getGameMode().getDisplayName()));
                    player.sendMessage(Component.text("§7Players: §f" + arena.getPlayers().size() +
                            "/" + arena.getMaxPlayers()));
                    player.sendMessage(Component.text("§7Min Players: §f" + arena.getMinPlayers()));
                    player.sendMessage(Component.text("§7Setup: " +
                            (arena.isSetup() ? "§a✓ Complete" : "§c✗ Incomplete")));
                });
    }

    private @NonNull Component msg(@NonNull String key) {
        ConfigurationProvider messages = plugin.getRegisteredConfig("messages").orElse(null);
        if (messages == null) {
            return Component.text("Missing messages config!");
        }

        String msg = messages.getString("messages." + key);
        if (msg == null) {
            return Component.text("Missing message: " + key);
        }

        String prefix = messages.getString("prefix", "§6[DodgeBall] §r");
        return Component.text(prefix + msg);
    }

    private void sendHelpMessage(@NonNull Player player) {
        player.sendMessage(Component.text("§6§l═══════════════════════════════"));
        player.sendMessage(Component.text("§e      DodgeBall Commands"));
        player.sendMessage(Component.text("§6§l═══════════════════════════════"));
        player.sendMessage(Component.text("§b/dodgeball join <arena> §7- Join an arena"));
        player.sendMessage(Component.text("§b/dodgeball leave §7- Leave current arena"));
        player.sendMessage(Component.text("§b/dodgeball list §7- List all arenas"));
        player.sendMessage(Component.text("§b/dodgeball stats [player] §7- View statistics"));
        player.sendMessage(Component.text("§6§l═══════════════════════════════"));
    }

    private void sendAdminHelpMessage(@NonNull Player player) {
        player.sendMessage(Component.text("§6§l═══════════════════════════════"));
        player.sendMessage(Component.text("§c   DodgeBall Admin Commands"));
        player.sendMessage(Component.text("§6§l═══════════════════════════════"));
        player.sendMessage(Component.text("§b/dba create <id> <name> <min> <max> <mode>"));
        player.sendMessage(Component.text("§b/dba delete <arena>"));
        player.sendMessage(Component.text("§b/dba setup <arena> <type>"));
        player.sendMessage(Component.text("§b/dba start <arena>"));
        player.sendMessage(Component.text("§b/dba stop <arena>"));
        player.sendMessage(Component.text("§b/dba info <arena>"));
        player.sendMessage(Component.text("§b/dba reload"));
        player.sendMessage(Component.text("§6§l═══════════════════════════════"));
    }
}