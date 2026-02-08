package me.sunmc.dodgeball.player;


import me.sunmc.dodgeball.DodgeBall;
import me.sunmc.dodgeball.ball.Ball;
import me.sunmc.dodgeball.stats.PlayerStats;
import me.sunmc.dodgeball.team.Team;
import me.sunmc.tools.configuration.ConfigurationProvider;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

/**
 * Player wrapper - FULLY IMPLEMENTED
 */
public class DodgeBallPlayer {

    private final @NonNull Player player;
    private final @NonNull UUID uuid;
    private final @NonNull PlayerStats stats;
    private final @NonNull DodgeBall plugin;

    private @Nullable Team team;
    private boolean alive;
    private boolean canCatch;
    private long lastCatchTime;
    private int ballsThrown;
    private int ballsCaught;
    private int successfulHits;

    public DodgeBallPlayer(@NonNull Player player, @NonNull DodgeBall plugin) {
        this.player = player;
        this.uuid = player.getUniqueId();
        this.plugin = plugin;
        this.stats = new PlayerStats(uuid, plugin);
        reset();
    }

    public void reset() {
        this.alive = true;
        this.canCatch = true;
        this.lastCatchTime = 0;
        this.ballsThrown = 0;
        this.ballsCaught = 0;
        this.successfulHits = 0;
    }

    public void onHit(@NonNull DodgeBallPlayer thrower, @NonNull Ball ball) {
        this.alive = false;
        stats.incrementDeaths();

        ConfigurationProvider messages = plugin.getRegisteredConfig("messages").orElse(null);
        if (messages != null) {
            String msg = messages.getString("§cYou were hit by {player}!",
                            "messages.hit-by-player")
                    .replace("{player}", thrower.getPlayer().getName());
            player.sendMessage(msg);
        }
    }

    public void onSuccessfulHit(@NonNull DodgeBallPlayer target) {
        successfulHits++;
        stats.incrementKills();

        ConfigurationProvider messages = plugin.getRegisteredConfig("messages").orElse(null);
        if (messages != null) {
            String msg = messages.getString("§aYou hit {player}!",
                            "messages.hit-player")
                    .replace("{player}", target.getPlayer().getName());
            player.sendMessage(msg);
        }
    }

    public void onCatch(@NonNull Ball ball) {
        ballsCaught++;
        stats.incrementCatches();
        lastCatchTime = System.currentTimeMillis();

        ConfigurationProvider messages = plugin.getRegisteredConfig("messages").orElse(null);
        if (messages != null) {
            String msg = messages.getString("§6Nice catch!", "messages.catch-success");
            player.sendMessage(msg);
        }
    }

    public void onBallCaught(@NonNull DodgeBallPlayer catcher) {
        ConfigurationProvider messages = plugin.getRegisteredConfig("messages").orElse(null);
        if (messages != null) {
            String msg = messages.getString("§c{player} caught your ball!",
                            "messages.ball-caught")
                    .replace("{player}", catcher.getPlayer().getName());
            player.sendMessage(msg);
        }
    }

    public void load() {
        stats.load();
    }

    public void save() {
        stats.save();
    }

    public @NonNull Player getPlayer() {
        return player;
    }

    public @NonNull UUID getUuid() {
        return uuid;
    }

    public @NonNull PlayerStats getStats() {
        return stats;
    }

    public me.sunmc.dodgeball.team.@Nullable Team getTeam() {
        return team;
    }

    public void setTeam(@Nullable Team team) {
        this.team = team;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean canCatch() {
        return canCatch && alive;
    }

    public void setCanCatch(boolean canCatch) {
        this.canCatch = canCatch;
    }

    public int getBallsThrown() {
        return ballsThrown;
    }

    public int getBallsCaught() {
        return ballsCaught;
    }

    public int getSuccessfulHits() {
        return successfulHits;
    }
}