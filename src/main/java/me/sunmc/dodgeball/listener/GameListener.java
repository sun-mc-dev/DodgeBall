package me.sunmc.dodgeball.listener;

import me.sunmc.dodgeball.DodgeBall;
import me.sunmc.dodgeball.arena.Arena;
import me.sunmc.dodgeball.arena.ArenaState;
import me.sunmc.dodgeball.ball.Ball;
import me.sunmc.dodgeball.component.ArenaManager;
import me.sunmc.dodgeball.component.BallManager;
import me.sunmc.dodgeball.component.PlayerManager;
import me.sunmc.dodgeball.player.DodgeBallPlayer;
import me.sunmc.tools.Tools;
import me.sunmc.tools.registry.AutoRegister;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Main game event listener - FULLY IMPLEMENTED
 */
@AutoRegister(Listener.class)
public class GameListener implements Listener {

    private final @NonNull DodgeBall plugin;

    public GameListener(@NonNull DodgeBall plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(@NonNull PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player data
        PlayerManager manager = Tools.getComponent(PlayerManager.class);
        manager.loadPlayer(player);
    }

    @EventHandler
    public void onPlayerQuit(@NonNull PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remove from arena
        ArenaManager arenaManager = Tools.getComponent(ArenaManager.class);
        arenaManager.removePlayer(player.getUniqueId());

        // Unload player data
        PlayerManager playerManager = Tools.getComponent(PlayerManager.class);
        playerManager.unloadPlayer(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(@NonNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.SNOWBALL) {
            return;
        }

        // Check if in arena
        ArenaManager arenaManager = Tools.getComponent(ArenaManager.class);
        Arena arena = arenaManager.getPlayerArena(player);

        if (arena == null || arena.getState() != ArenaState.IN_GAME) {
            return;
        }

        PlayerManager playerManager = Tools.getComponent(PlayerManager.class);
        DodgeBallPlayer dbPlayer = playerManager.getPlayer(player);

        if (!dbPlayer.isAlive()) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("§cYou are eliminated!"));
            return;
        }

        // Cancel default snowball
        event.setCancelled(true);

        // Throw custom ball
        throwBall(player, arena, dbPlayer, item);
    }

    private void throwBall(@NonNull Player player, @NonNull Arena arena,
                           @NonNull DodgeBallPlayer dbPlayer, @NonNull ItemStack ballItem) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        Vector velocity = direction.multiply(arena.getSettings().getBallSpeed());

        // Create ball
        Ball ball = new Ball(arena, dbPlayer, eyeLoc, velocity, ballItem);

        // Spawn ball
        BallManager ballManager = Tools.getComponent(BallManager.class);
        ballManager.spawnBall(ball);

        // Remove snowball
        if (ballItem.getAmount() > 1) {
            ballItem.setAmount(ballItem.getAmount() - 1);
        } else {
            player.getInventory().remove(ballItem);
        }

        // Update stats
        dbPlayer.getStats().incrementShoots();

        // Play sound
        player.playSound(player.getLocation(),
                org.bukkit.Sound.ENTITY_SNOWBALL_THROW, 1.0f, 1.0f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(@NonNull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ArenaManager arenaManager = Tools.getComponent(ArenaManager.class);
        Arena arena = arenaManager.getPlayerArena(player);

        if (arena != null && arena.getState() == ArenaState.IN_GAME) {
            // Cancel all damage in arena
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDamageByEntity(@NonNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ArenaManager arenaManager = Tools.getComponent(ArenaManager.class);
        Arena arena = arenaManager.getPlayerArena(player);

        if (arena != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(@NonNull PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();

        if (to == null) return;

        ArenaManager arenaManager = Tools.getComponent(ArenaManager.class);
        Arena arena = arenaManager.getPlayerArena(player);

        if (arena == null || arena.getState() != ArenaState.IN_GAME) {
            return;
        }

        // Check bounds
        Location min = arena.getLocation("MIN_BOUND");
        Location max = arena.getLocation("MAX_BOUND");

        if (min != null && max != null) {
            if (to.getX() < min.getX() || to.getX() > max.getX() ||
                    to.getY() < min.getY() || to.getY() > max.getY() ||
                    to.getZ() < min.getZ() || to.getZ() > max.getZ()) {

                event.setCancelled(true);
                player.sendActionBar(Component.text("§cYou can't leave the arena!"));
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(@NonNull FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ArenaManager arenaManager = Tools.getComponent(ArenaManager.class);
        Arena arena = arenaManager.getPlayerArena(player);

        if (arena != null && arena.getState() == ArenaState.IN_GAME) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(@NonNull BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE && player.hasPermission("dodgeball.admin")) {
            return;
        }

        ArenaManager arenaManager = Tools.getComponent(ArenaManager.class);
        Arena arena = arenaManager.getPlayerArena(player);

        if (arena != null) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("§cYou can't break blocks in the arena!"));
        }
    }

    @EventHandler
    public void onBlockPlace(@NonNull BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE && player.hasPermission("dodgeball.admin")) {
            return;
        }

        ArenaManager arenaManager = Tools.getComponent(ArenaManager.class);
        Arena arena = arenaManager.getPlayerArena(player);

        if (arena != null) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("§cYou can't place blocks in the arena!"));
        }
    }

    @EventHandler
    public void onPlayerDropItem(@NonNull PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        ArenaManager arenaManager = Tools.getComponent(ArenaManager.class);
        Arena arena = arenaManager.getPlayerArena(player);

        if (arena != null && arena.getState() == ArenaState.IN_GAME) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickupItem(@NonNull PlayerPickupArrowEvent event) {
        Player player = event.getPlayer();

        ArenaManager arenaManager = Tools.getComponent(ArenaManager.class);
        Arena arena = arenaManager.getPlayerArena(player);

        if (arena != null && arena.getState() == ArenaState.IN_GAME) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(@NonNull PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();

        // Allow dodgeball commands
        if (command.startsWith("/dodgeball") || command.startsWith("/db") ||
                command.startsWith("/dodge") || command.startsWith("/dba")) {
            return;
        }

        // Block other commands in game
        ArenaManager arenaManager = Tools.getComponent(ArenaManager.class);
        Arena arena = arenaManager.getPlayerArena(player);

        if (arena != null && arena.getState() == ArenaState.IN_GAME) {
            if (!player.hasPermission("dodgeball.admin")) {
                event.setCancelled(true);
                player.sendMessage(Component.text("§cYou can't use commands during the game!"));
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(@NonNull PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        // Allow teleports caused by the plugin
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }

        ArenaManager arenaManager = Tools.getComponent(ArenaManager.class);
        Arena arena = arenaManager.getPlayerArena(player);

        if (arena != null && arena.getState() == ArenaState.IN_GAME) {
            if (!player.hasPermission("dodgeball.admin")) {
                event.setCancelled(true);
                player.sendMessage(Component.text("§cYou can't teleport during the game!"));
            }
        }
    }
}