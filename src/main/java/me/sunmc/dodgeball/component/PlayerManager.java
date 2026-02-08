package me.sunmc.dodgeball.component;


import me.sunmc.dodgeball.DodgeBall;
import me.sunmc.dodgeball.player.DodgeBallPlayer;
import me.sunmc.tools.component.Component;
import me.sunmc.tools.registry.AutoRegister;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all players - FULLY IMPLEMENTED
 */
@AutoRegister(Component.class)
public class PlayerManager implements Component {

    private final @NonNull DodgeBall plugin;
    private final @NonNull Map<UUID, DodgeBallPlayer> players;

    public PlayerManager(@NonNull DodgeBall plugin) {
        this.plugin = plugin;
        this.players = new ConcurrentHashMap<>();
    }

    @Override
    public void onEnable() {
        // Load existing online players
        Bukkit.getOnlinePlayers().forEach(this::loadPlayer);
    }

    @Override
    public void onDisable() {
        // Save all player data
        players.values().forEach(DodgeBallPlayer::save);
        players.clear();
    }

    public void loadPlayer(@NonNull Player player) {
        if (!players.containsKey(player.getUniqueId())) {
            DodgeBallPlayer dbPlayer = new DodgeBallPlayer(player, plugin);
            dbPlayer.load();
            players.put(player.getUniqueId(), dbPlayer);
        }
    }

    public void unloadPlayer(@NonNull UUID playerId) {
        DodgeBallPlayer player = players.remove(playerId);
        if (player != null) {
            player.save();
        }
    }

    public @NonNull DodgeBallPlayer getPlayer(@NonNull Player player) {
        return players.computeIfAbsent(player.getUniqueId(),
                k -> new DodgeBallPlayer(player, plugin));
    }

    public @Nullable DodgeBallPlayer getPlayer(@NonNull UUID playerId) {
        return players.get(playerId);
    }

    public @NonNull Collection<DodgeBallPlayer> getPlayers() {
        return new ArrayList<>(players.values());
    }
}