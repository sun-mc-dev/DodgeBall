package me.sunmc.dodgeball.component;


import me.sunmc.dodgeball.DodgeBall;
import me.sunmc.dodgeball.ball.Ball;
import me.sunmc.tools.component.Component;
import me.sunmc.tools.component.DependencyComponent;
import me.sunmc.tools.registry.AutoRegister;
import org.bukkit.Bukkit;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.foreign.Arena;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages all active balls - FULLY IMPLEMENTED
 */
@AutoRegister(Component.class)
@DependencyComponent({ArenaManager.class})
public class BallManager implements Component {

    private final @NonNull DodgeBall plugin;
    private final @NonNull List<Ball> activeBalls;
    private int taskId = -1;

    public BallManager(@NonNull DodgeBall plugin) {
        this.plugin = plugin;
        this.activeBalls = new CopyOnWriteArrayList<>();
    }

    @Override
    public void onEnable() {
        // Start ball physics task
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,
                this::tickAllBalls, 0L, 1L);

        plugin.getLogger().info("Ball manager enabled with physics task");
    }

    @Override
    public void onDisable() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        activeBalls.forEach(Ball::despawnForAll);
        activeBalls.clear();
    }

    public void spawnBall(@NonNull Ball ball) {
        activeBalls.add(ball);
        ball.getArena().getPlayers().forEach(p -> ball.spawnForPlayer(p.getPlayer()));
    }

    public void removeBall(@NonNull Ball ball) {
        activeBalls.remove(ball);
        ball.despawnForAll();
    }

    private void tickAllBalls() {
        activeBalls.parallelStream().forEach(ball -> {
            try {
                ball.tick();
                if (!ball.isActive()) {
                    activeBalls.remove(ball);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error ticking ball: " + e.getMessage());
                activeBalls.remove(ball);
            }
        });
    }

    public @NonNull List<Ball> getActiveBalls() {
        return new ArrayList<>(activeBalls);
    }

    public @NonNull List<Ball> getArenaBalls(@NonNull Arena arena) {
        return activeBalls.stream()
                .filter(ball -> ball.getArena().equals(arena))
                .toList();
    }
}
