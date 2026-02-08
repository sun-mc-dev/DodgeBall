package me.sunmc.dodgeball.ball;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityMetadataProvider;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.sunmc.dodgeball.arena.Arena;
import me.sunmc.dodgeball.player.DodgeBallPlayer;
import me.sunmc.dodgeball.team.Team;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ball with PacketEvents - FULLY IMPLEMENTED
 */
public class Ball {

    private static final @NonNull AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(100000);
    private static final double GRAVITY = 0.03;
    private static final double AIR_RESISTANCE = 0.99;
    private static final double BOUNCE_FACTOR = 0.6;
    private static final double HIT_RADIUS = 0.8;

    private final int entityId;
    private final @NonNull UUID ballId;
    private final @NonNull Arena arena;
    private final @Nullable DodgeBallPlayer thrower;
    private final @Nullable Team team;

    private @NonNull Location location;
    private @NonNull Vector velocity;
    private final @NonNull ItemStack ballItem;

    private boolean active;
    private long spawnTime;
    private int ticksLived;
    private boolean onGround;

    private final @NonNull Set<UUID> viewers;
    private final @NonNull Map<UUID, Long> lastHitPlayers;

    public Ball(
            @NonNull Arena arena,
            @Nullable DodgeBallPlayer thrower,
            @NonNull Location spawnLocation,
            @NonNull Vector initialVelocity,
            @NonNull ItemStack ballItem
    ) {
        this.entityId = ENTITY_ID_COUNTER.incrementAndGet();
        this.ballId = UUID.randomUUID();
        this.arena = arena;
        this.thrower = thrower;
        this.team = thrower != null ? thrower.getTeam() : null;

        this.location = spawnLocation.clone();
        this.velocity = initialVelocity.clone();
        this.ballItem = ballItem.clone();

        this.active = true;
        this.spawnTime = System.currentTimeMillis();
        this.ticksLived = 0;
        this.onGround = false;

        this.viewers = ConcurrentHashMap.newKeySet();
        this.lastHitPlayers = new ConcurrentHashMap<>();
    }

    public void spawnForPlayer(@NonNull Player player) {
        if (viewers.contains(player.getUniqueId())) {
            return;
        }

        try {
            // Spawn armor stand entity
            WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                    entityId,
                    Optional.of(UUID.randomUUID()),
                    EntityTypes.ARMOR_STAND,
                    SpigotConversionUtil.fromBukkitLocation(location).getPosition(),
                    0f, 0f, 0f,
                    0,
                    Optional.empty()
            );

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);

            // Set metadata (invisible, small, marker, no gravity)
            List<EntityData> metadata = new ArrayList<>();

            // Entity flags (invisible)
            metadata.add(new EntityData(
                    0,
                    EntityDataTypes.BYTE,
                    (byte) 0x20
            ));

            // Armor stand flags (small, marker)
            metadata.add(new EntityData(
                    15,
                    EntityDataTypes.BYTE,
                    (byte) (0x01 | 0x10)
            ));

            WrapperPlayServerEntityMetadata metadataPacket =
                    new WrapperPlayServerEntityMetadata(entityId, metadata);

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadataPacket);

            // Equip ball item on head
            WrapperPlayServerEntityEquipment equipmentPacket = new WrapperPlayServerEntityEquipment(
                    entityId,
                    List.of(new Equipment(
                            EquipmentSlot.HELMET,
                            SpigotConversionUtil.fromBukkitItemStack(ballItem)
                    ))
            );

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, equipmentPacket);

            viewers.add(player.getUniqueId());

        } catch (Exception e) {
            // Silently fail - player might have disconnected
        }
    }

    public void despawnForPlayer(@NonNull Player player) {
        if (!viewers.contains(player.getUniqueId())) {
            return;
        }

        try {
            WrapperPlayServerDestroyEntities destroyPacket =
                    new WrapperPlayServerDestroyEntities(entityId);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyPacket);
        } catch (Exception e) {
            // Silently fail
        }

        viewers.remove(player.getUniqueId());
    }

    public void despawnForAll() {
        arena.getPlayers().forEach(p -> despawnForPlayer(p.getPlayer()));
        viewers.clear();
        active = false;
    }


    public void tick() {
        if (!active) {
            return;
        }

        ticksLived++;

        // Apply gravity
        if (!onGround) {
            velocity.setY(velocity.getY() - GRAVITY);
        }

        // Apply air resistance
        velocity.multiply(AIR_RESISTANCE);

        // Move ball
        Location newLocation = location.clone().add(velocity);

        // Check ground collision
        if (newLocation.getY() <= location.getWorld().getMinHeight() + 1 ||
                newLocation.getBlock().getType().isSolid()) {
            handleGroundCollision();
        } else {
            location = newLocation;
        }

        // Check player collisions
        checkPlayerCollisions();

        // Update position
        updatePosition();

        // Spawn particle trail
        if (ticksLived % 2 == 0 && velocity.lengthSquared() > 0.01) {
            spawnParticleTrail();
        }

        // Despawn conditions
        if (ticksLived > 200 || (onGround && velocity.lengthSquared() < 0.001)) {
            despawnForAll();
        }
    }

    private void handleGroundCollision() {
        if (!onGround) {
            onGround = true;

            // Bounce
            velocity.setY(Math.abs(velocity.getY()) * BOUNCE_FACTOR);

            if (velocity.getY() < 0.1) {
                velocity.setY(0);
                velocity.multiply(0.8);
            }

            // Play bounce sound
            location.getWorld().playSound(location, Sound.ENTITY_SLIME_SQUISH, 0.5f, 1.5f);
        } else {
            velocity.setY(0);
            velocity.multiply(0.9);
        }
    }

    private void checkPlayerCollisions() {
        for (DodgeBallPlayer player : arena.getPlayers()) {
            if (!player.isAlive()) {
                continue;
            }

            Player bukkitPlayer = player.getPlayer();
            Location playerLoc = bukkitPlayer.getEyeLocation();

            // Check distance
            double distance = location.distance(playerLoc);
            if (distance < HIT_RADIUS) {
                handlePlayerHit(player);
                break;
            }
        }
    }

    private void handlePlayerHit(@NonNull DodgeBallPlayer player) {
        // Debounce
        Long lastHit = lastHitPlayers.get(player.getUuid());
        if (lastHit != null && System.currentTimeMillis() - lastHit < 500) {
            return;
        }

        // Can't hit thrower immediately
        if (thrower != null && player.equals(thrower) && ticksLived < 5) {
            return;
        }

        // Can't hit teammates (allow catching)
        if (thrower != null && player.getTeam() == team) {
            if (player.canCatch() && velocity.lengthSquared() > 0.1) {
                handleCatch(player);
                return;
            }
            return;
        }

        // Neutral ball can be picked up
        if (thrower == null) {
            handlePickup(player);
            return;
        }

        // Hit opponent
        lastHitPlayers.put(player.getUuid(), System.currentTimeMillis());

        player.onHit(thrower, this);
        thrower.onSuccessfulHit(player);

        spawnHitEffect();
        despawnForAll();
    }

    private void handleCatch(@NonNull DodgeBallPlayer catcher) {
        if (thrower == null) return;

        catcher.onCatch(this);
        thrower.onBallCaught(catcher);

        spawnCatchEffect();
        despawnForAll();

        // Give catcher a ball
        catcher.getPlayer().getInventory().addItem(ballItem.clone());
    }

    private void handlePickup(@NonNull DodgeBallPlayer picker) {
        picker.getPlayer().getInventory().addItem(ballItem.clone());
        picker.getPlayer().playSound(picker.getPlayer().getLocation(),
                Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
        despawnForAll();
    }

    private void updatePosition() {
        Vector3d position = new Vector3d(location.getX(), location.getY(), location.getZ());

        WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(
                entityId,
                position,
                location.getYaw(),
                location.getPitch(),
                false
        );

        for (UUID viewerId : viewers) {
            Player viewer = arena.getPlayers().stream()
                    .map(DodgeBallPlayer::getPlayer)
                    .filter(p -> p.getUniqueId().equals(viewerId))
                    .findFirst()
                    .orElse(null);

            if (viewer != null) {
                try {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, teleportPacket);
                } catch (Exception e) {
                    // Player disconnected
                    viewers.remove(viewerId);
                }
            }
        }
    }


    private void spawnParticleTrail() {
        if (team == Team.RED) {
            location.getWorld().spawnParticle(Particle.DUST, location, 1,
                    new Particle.DustOptions(org.bukkit.Color.RED, 0.5f));
        } else if (team == Team.BLUE) {
            location.getWorld().spawnParticle(Particle.DUST, location, 1,
                    new Particle.DustOptions(org.bukkit.Color.BLUE, 0.5f));
        } else {
            location.getWorld().spawnParticle(Particle.DUST, location, 1,
                    new Particle.DustOptions(org.bukkit.Color.WHITE, 0.5f));
        }
    }

    private void spawnHitEffect() {
        location.getWorld().spawnParticle(Particle.EXPLOSION, location, 3);
        arena.getPlayers().forEach(p ->
                p.getPlayer().playSound(location, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.8f)
        );
    }

    private void spawnCatchEffect() {
        location.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location, 10, 0.5, 0.5, 0.5);
        arena.getPlayers().forEach(p ->
                p.getPlayer().playSound(location, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.5f)
        );
    }


    public int getEntityId() { return entityId; }
    public @NonNull UUID getBallId() { return ballId; }
    public @NonNull Arena getArena() { return arena; }
    public @Nullable DodgeBallPlayer getThrower() { return thrower; }
    public @Nullable Team getTeam() { return team; }
    public @NonNull Location getLocation() { return location.clone(); }
    public @NonNull Vector getVelocity() { return velocity.clone(); }
    public boolean isActive() { return active; }
    public int getTicksLived() { return ticksLived; }
    public long getLifetime() { return System.currentTimeMillis() - spawnTime; }
}