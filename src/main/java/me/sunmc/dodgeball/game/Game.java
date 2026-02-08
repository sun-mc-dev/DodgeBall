package me.sunmc.dodgeball.game;


import me.sunmc.dodgeball.DodgeBall;
import me.sunmc.dodgeball.arena.Arena;
import me.sunmc.dodgeball.arena.ArenaState;
import me.sunmc.dodgeball.ball.Ball;
import me.sunmc.dodgeball.component.BallManager;
import me.sunmc.dodgeball.player.DodgeBallPlayer;
import me.sunmc.dodgeball.team.Team;
import me.sunmc.tools.Tools;
import me.sunmc.tools.item.util.ItemStackBuilder;
import me.sunmc.tools.scheduler.timer.SimpleTimer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Game instance - FULLY IMPLEMENTED
 */
public class Game {

    private final @NonNull Arena arena;
    private final @NonNull PlayMode gameMode;
    private final @NonNull SimpleTimer gameTimer;
    private final @NonNull DodgeBall plugin;

    private @Nullable Team winner;
    private long startTime;
    private boolean active;

    public Game(@NonNull Arena arena, @NonNull PlayMode gameMode, @NonNull DodgeBall plugin) {
        this.arena = arena;
        this.gameMode = gameMode;
        this.plugin = plugin;
        this.active = false;

        this.gameTimer = new SimpleTimer("game-" + arena.getArenaId(),
                arena.getSettings().getGameDuration(), 0);

        gameTimer.setInterval(1, TimeUnit.SECONDS);
        gameTimer.onTick(this::onTimerTick);
        gameTimer.onComplete(this::onTimeUp);
    }

    /**
     * Starts the game - FULLY IMPLEMENTED
     */
    public void start() {
        active = true;
        startTime = System.currentTimeMillis();

        arena.setState(ArenaState.IN_GAME);

        // Teleport players to spawns
        teleportPlayersToSpawns();

        // Give starting equipment
        giveStartingEquipment();

        // Start timer
        gameTimer.startTimer(SimpleTimer.TimeChange.DECREMENT);

        // Spawn initial balls
        spawnInitialBalls();

        // Broadcast start
        arena.broadcast(Component.text("§a§lGAME STARTED!"));
        arena.getPlayers().forEach(p -> {
            p.getPlayer().playSound(p.getPlayer().getLocation(),
                    Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);

            p.getPlayer().showTitle(Title.title(
                    Component.text("§a§lSTART!"),
                    Component.text("§7Good luck!"),
                    Title.Times.times(
                            Duration.ofMillis(500),
                            Duration.ofMillis(2000),
                            Duration.ofMillis(500)
                    )
            ));
        });
    }

    /**
     * Ends the game - FULLY IMPLEMENTED
     */
    public void end() {
        active = false;
        gameTimer.stopTimer();

        arena.setState(ArenaState.ENDING);

        // Show results
        showResults();

        // Update statistics
        updateStatistics();

        // Clean up balls
        cleanup();

        // Schedule arena reset
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            arena.setState(ArenaState.RESETTING);

            // Teleport players to lobby
            Location lobby = arena.getLocation("LOBBY");
            if (lobby != null) {
                arena.getPlayers().forEach(p -> p.getPlayer().teleport(lobby));
            }

            // Reset arena
            arena.setState(ArenaState.WAITING);
        }, 200L); // 10 seconds
    }

    private void teleportPlayersToSpawns() {
        Location redSpawn = arena.getLocation("TEAM1_SPAWN");
        Location blueSpawn = arena.getLocation("TEAM2_SPAWN");

        if (redSpawn == null || blueSpawn == null) {
            plugin.getLogger().warning("Arena " + arena.getArenaId() + " missing spawn locations!");
            return;
        }

        for (DodgeBallPlayer player : arena.getTeamPlayers(Team.RED)) {
            player.getPlayer().teleport(redSpawn);
        }

        for (DodgeBallPlayer player : arena.getTeamPlayers(Team.BLUE)) {
            player.getPlayer().teleport(blueSpawn);
        }
    }

    private void giveStartingEquipment() {
        ItemStack ballItem = createBallItem();

        arena.getPlayers().forEach(player -> {
            player.getPlayer().getInventory().clear();
            player.getPlayer().getInventory().addItem(ballItem.clone(), ballItem.clone(), ballItem.clone());
        });
    }

    private void spawnInitialBalls() {
        Location center = arena.getLocation("CENTER");
        if (center == null) return;

        BallManager ballManager = Tools.getComponent(BallManager.class);
        ItemStack ballItem = createBallItem();

        // Spawn 5 neutral balls at center
        for (int i = 0; i < 5; i++) {
            Location spawnLoc = center.clone().add(
                    (Math.random() * 4) - 2,
                    1,
                    (Math.random() * 4) - 2
            );

            // Create a stationary ball
            Ball ball = new Ball(
                    arena,
                    null, // No thrower for neutral balls
                    spawnLoc,
                    new Vector(0, 0, 0),
                    ballItem
            );

            ballManager.spawnBall(ball);
        }
    }

    private @NonNull ItemStack createBallItem() {
        int customModelData = arena.getSettings().getBallCustomModelData();

        return ItemStackBuilder.of(Material.SNOWBALL)
                .name("§eDodgeBall", true)
                .lore(true,
                        "§7Throw at enemies!",
                        "§7Right-click to throw"
                )
                .customModelData(customModelData)
                .build();
    }

    private void onTimerTick(int secondsLeft) {
        // Show time warnings
        if (secondsLeft == 60 || secondsLeft == 30 || secondsLeft == 10 || secondsLeft == 5) {
            arena.broadcast(Component.text("§e" + secondsLeft + " seconds remaining!"));
            arena.getPlayers().forEach(p ->
                    p.getPlayer().playSound(p.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
            );
        }

        // Update action bar
        arena.getPlayers().forEach(p -> {
            p.getPlayer().sendActionBar(
                    Component.text("§6Time: §f" + formatTime(secondsLeft) +
                            " §8| §cRed: §f" + getAliveCount(Team.RED) +
                            " §8| §9Blue: §f" + getAliveCount(Team.BLUE))
            );
        });
    }

    private void onTimeUp() {
        arena.broadcast(Component.text("§c§lTIME'S UP!"));

        // Determine winner by alive players
        int redAlive = getAliveCount(Team.RED);
        int blueAlive = getAliveCount(Team.BLUE);

        if (redAlive > blueAlive) {
            winner = Team.RED;
        } else if (blueAlive > redAlive) {
            winner = Team.BLUE;
        }
        // else draw (winner remains null)

        end();
    }

    private int getAliveCount(@NonNull Team team) {
        return (int) arena.getTeamPlayers(team).stream()
                .filter(DodgeBallPlayer::isAlive)
                .count();
    }

    private void showResults() {
        Component winMessage;

        if (winner != null) {
            winMessage = Component.text("§a§l" + winner.getDisplayName().toUpperCase() + " WINS!");
        } else {
            winMessage = Component.text("§e§lDRAW!");
        }

        arena.broadcast(Component.empty());
        arena.broadcast(winMessage);
        arena.broadcast(Component.empty());

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        arena.broadcast(Component.text("§7Duration: §f" + formatTime((int) duration)));

        // Show title
        arena.getPlayers().forEach(p -> {
            p.getPlayer().showTitle(Title.title(
                    winMessage,
                    Component.text("§7Game Over"),
                    Title.Times.times(
                            Duration.ofMillis(500),
                            Duration.ofMillis(3000),
                            Duration.ofMillis(500)
                    )
            ));

            p.getPlayer().playSound(p.getPlayer().getLocation(),
                    winner != null ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_VILLAGER_NO,
                    1.0f, 1.0f);
        });

        // Show individual stats
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            arena.broadcast(Component.text("§6§l=== Match Statistics ==="));
            arena.getPlayers().forEach(p -> {
                arena.broadcast(Component.text(
                        "§e" + p.getPlayer().getName() + " §8- " +
                                "§cK: " + p.getSuccessfulHits() + " " +
                                "§aC: " + p.getBallsCaught() + " " +
                                "§7T: " + p.getBallsThrown()
                ));
            });
        }, 60L);
    }

    private void updateStatistics() {
        long playtime = System.currentTimeMillis() - startTime;

        for (DodgeBallPlayer player : arena.getPlayers()) {
            // Update wins/losses
            if (winner != null && player.getTeam() == winner) {
                player.getStats().incrementWins();
            } else if (winner != null) {
                player.getStats().incrementLosses();
            }

            // Add playtime
            player.getStats().addPlaytime(playtime);

            // Save stats
            player.save();
        }
    }

    private void cleanup() {
        BallManager ballManager = Tools.getComponent(BallManager.class);
        ballManager.getArenaBalls(arena).forEach(Ball::despawnForAll);

        // Reset players
        arena.getPlayers().forEach(DodgeBallPlayer::reset);
    }

    private @NonNull String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    public @Nullable Team getWinner() {
        return winner;
    }

    // Getters and setters
    public void setWinner(@Nullable Team winner) {
        this.winner = winner;
    }

    public boolean isActive() {
        return active;
    }

    public @NonNull Arena getArena() {
        return arena;
    }
}