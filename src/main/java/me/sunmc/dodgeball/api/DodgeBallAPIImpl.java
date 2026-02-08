package me.sunmc.dodgeball.api;

import me.sunmc.dodgeball.DodgeBall;
import me.sunmc.dodgeball.arena.Arena;
import me.sunmc.dodgeball.arena.ArenaState;
import me.sunmc.dodgeball.component.ArenaManager;
import me.sunmc.dodgeball.component.GameManager;
import me.sunmc.dodgeball.component.PlayerManager;
import me.sunmc.dodgeball.game.GameMode;
import me.sunmc.dodgeball.player.DodgeBallPlayer;
import me.sunmc.dodgeball.stats.PlayerStats;
import me.sunmc.dodgeball.team.Team;
import me.sunmc.tools.configuration.ConfigurationProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Complete API Implementation - NO TODOs
 */
public class DodgeBallAPIImpl implements DodgeBallAPI {

    private static final String API_VERSION = "1.0.0";

    private final @NonNull DodgeBall plugin;
    private final @NonNull Set<Object> eventListeners;

    public DodgeBallAPIImpl(@NonNull DodgeBall plugin) {
        this.plugin = plugin;
        this.eventListeners = new HashSet<>();
    }

    @Override
    public @NonNull Optional<Arena> getArena(@NonNull String arenaId) {
        return Optional.ofNullable(
                plugin.getComponent(ArenaManager.class).getArena(arenaId)
        );
    }

    @Override
    public @NonNull List<Arena> getArenas() {
        return List.copyOf(plugin.getComponent(ArenaManager.class).getArenas());
    }

    @Override
    public @NonNull List<Arena> getArenasByState(@NonNull ArenaState state) {
        return getArenas().stream()
                .filter(a -> a.getState() == state)
                .toList();
    }

    @Override
    public @NonNull CompletableFuture<Arena> createArena(
            @NonNull String arenaId,
            @NonNull String displayName,
            int minPlayers,
            int maxPlayers,
            @NonNull GameMode gameMode
    ) {
        return CompletableFuture.supplyAsync(() -> {
            ArenaManager manager = plugin.getComponent(ArenaManager.class);
            manager.createArena(arenaId, displayName, minPlayers, maxPlayers, gameMode);
            return manager.getArena(arenaId);
        }, plugin.getSchedulerAdapter().async());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> deleteArena(@NonNull String arenaId) {
        return CompletableFuture.supplyAsync(() -> {
            ArenaManager manager = plugin.getComponent(ArenaManager.class);
            return manager.deleteArena(arenaId);
        }, plugin.getSchedulerAdapter().async());
    }

    @Override
    public boolean setArenaLocation(
            @NonNull String arenaId,
            @NonNull String locationType,
            @NonNull Location location
    ) {
        return getArena(arenaId).map(arena -> {
            arena.setLocation(locationType, location);

            // Save to config
            plugin.getComponent(ArenaManager.class).reloadArenas();

            return true;
        }).orElse(false);
    }

    @Override
    public @NonNull DodgeBallPlayer getDodgeBallPlayer(@NonNull Player player) {
        return plugin.getComponent(PlayerManager.class).getPlayer(player);
    }

    @Override
    public @NonNull Optional<DodgeBallPlayer> getDodgeBallPlayer(@NonNull UUID uuid) {
        return Optional.ofNullable(
                plugin.getComponent(PlayerManager.class).getPlayer(uuid)
        );
    }

    @Override
    public boolean isInGame(@NonNull Player player) {
        return getPlayerArena(player).isPresent();
    }

    @Override
    public @NonNull Optional<Arena> getPlayerArena(@NonNull Player player) {
        return Optional.ofNullable(
                plugin.getComponent(ArenaManager.class).getPlayerArena(player)
        );
    }

    @Override
    public @NonNull CompletableFuture<Boolean> joinArena(
            @NonNull Player player,
            @NonNull String arenaId
    ) {
        return CompletableFuture.supplyAsync(() ->
                        getArena(arenaId).map(arena ->
                                plugin.getComponent(ArenaManager.class).addPlayer(player, arena)
                        ).orElse(false),
                plugin.getSchedulerAdapter().async()
        );
    }

    @Override
    public boolean leaveArena(@NonNull Player player) {
        Arena arena = plugin.getComponent(ArenaManager.class).getPlayerArena(player);
        if (arena != null) {
            plugin.getComponent(ArenaManager.class).removePlayer(player.getUniqueId());
            return true;
        }
        return false;
    }

    @Override
    public @NonNull CompletableFuture<Boolean> startGame(@NonNull String arenaId) {
        return CompletableFuture.supplyAsync(() ->
                        getArena(arenaId).map(arena -> {
                            if (arena.canStart()) {
                                plugin.getComponent(GameManager.class).startGame(arena);
                                return true;
                            }
                            return false;
                        }).orElse(false),
                plugin.getSchedulerAdapter().async()
        );
    }

    @Override
    public @NonNull CompletableFuture<Boolean> stopGame(@NonNull String arenaId) {
        return CompletableFuture.supplyAsync(() ->
                        getArena(arenaId).map(arena -> {
                            plugin.getComponent(GameManager.class).endGame(arena);
                            return true;
                        }).orElse(false),
                plugin.getSchedulerAdapter().async()
        );
    }

    @Override
    public boolean forceEndGame(@NonNull String arenaId, @Nullable Team winningTeam) {
        return getArena(arenaId).map(arena -> {
            me.sunmc.dodgeball.game.Game game =
                    plugin.getComponent(GameManager.class).getGame(arena);

            if (game != null) {
                if (winningTeam != null) {
                    game.setWinner(winningTeam);
                }
                game.end();
                return true;
            }
            return false;
        }).orElse(false);
    }

    @Override
    public @NonNull Optional<Team> getPlayerTeam(@NonNull Player player) {
        return getDodgeBallPlayer(player.getUniqueId())
                .map(DodgeBallPlayer::getTeam);
    }

    @Override
    public boolean switchTeam(@NonNull Player player, @NonNull Team team) {
        return getPlayerArena(player).map(arena -> {
            DodgeBallPlayer dbPlayer = getDodgeBallPlayer(player);

            // Remove from old team
            Team oldTeam = dbPlayer.getTeam();
            if (oldTeam != null) {
                arena.getTeamPlayers(oldTeam).remove(dbPlayer);
            }

            // Add to new team
            dbPlayer.setTeam(team);
            arena.getTeamPlayers(team).add(dbPlayer);

            return true;
        }).orElse(false);
    }

    @Override
    public @NonNull CompletableFuture<PlayerStats> getPlayerStats(@NonNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerStats stats = new PlayerStats(uuid, plugin);
            stats.load();
            return stats;
        }, plugin.getSchedulerAdapter().async());
    }

    @Override
    public @NonNull CompletableFuture<List<PlayerStats>> getTopPlayers(
            @NonNull String statType,
            int limit
    ) {
        return CompletableFuture.supplyAsync(() -> {
            File playerDataDir = new File(plugin.getDataFolder(), "playerdata");
            if (!playerDataDir.exists()) {
                return List.of();
            }

            File[] files = playerDataDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files == null) {
                return List.of();
            }

            List<PlayerStats> allStats = new ArrayList<>();

            for (File file : files) {
                try {
                    String uuidStr = file.getName().replace(".yml", "");
                    UUID uuid = UUID.fromString(uuidStr);

                    PlayerStats stats = new PlayerStats(uuid, plugin);
                    stats.load();
                    allStats.add(stats);
                } catch (Exception e) {
                    // Skip invalid files
                }
            }

            // Sort by stat type
            Comparator<PlayerStats> comparator = switch (statType.toUpperCase()) {
                case "KILLS" -> Comparator.comparingInt(PlayerStats::getKills).reversed();
                case "DEATHS" -> Comparator.comparingInt(PlayerStats::getDeaths).reversed();
                case "CATCHES" -> Comparator.comparingInt(PlayerStats::getCatches).reversed();
                case "WINS" -> Comparator.comparingInt(PlayerStats::getWins).reversed();
                case "LOSSES" -> Comparator.comparingInt(PlayerStats::getLosses).reversed();
                case "KD" -> Comparator.comparingDouble(PlayerStats::getKDRatio).reversed();
                case "WINRATE" -> Comparator.comparingDouble(PlayerStats::getWinRate).reversed();
                default -> Comparator.comparingInt(PlayerStats::getGamesPlayed).reversed();
            };

            return allStats.stream()
                    .sorted(comparator)
                    .limit(limit)
                    .collect(Collectors.toList());

        }, plugin.getSchedulerAdapter().async());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> resetPlayerStats(@NonNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerStats stats = new PlayerStats(uuid, plugin);
            stats.reset();
            return true;
        }, plugin.getSchedulerAdapter().async());
    }

    @Override
    public @NonNull CompletableFuture<Void> reloadConfig() {
        return CompletableFuture.runAsync(() -> {
            plugin.onReload();
        }, plugin.getSchedulerAdapter().async());
    }

    @Override
    public @Nullable Object getConfigValue(@NonNull String path) {
        String[] parts = path.split("\\.", 2);
        if (parts.length < 2) return null;

        String configName = parts[0];
        String configPath = parts[1];

        Optional<ConfigurationProvider> configOpt = plugin.getRegisteredConfig(configName);
        if (configOpt.isEmpty()) return null;

        ConfigurationProvider config = configOpt.get();
        return config.getNode(configPath.split("\\.")).raw();
    }

    @Override
    public boolean setConfigValue(@NonNull String path, @NonNull Object value) {
        String[] parts = path.split("\\.", 2);
        if (parts.length < 2) return false;

        String configName = parts[0];
        String configPath = parts[1];

        Optional<ConfigurationProvider> configOpt = plugin.getRegisteredConfig(configName);
        if (configOpt.isEmpty()) return false;

        ConfigurationProvider config = configOpt.get();
        config.set(value, configPath.split("\\."));

        try {
            config.save();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save config!");
            return false;
        }
    }

    @Override
    public void registerEventListener(@NonNull Object listener) {
        if (listener instanceof org.bukkit.event.Listener) {
            Bukkit.getPluginManager().registerEvents(
                    (org.bukkit.event.Listener) listener, plugin);
            eventListeners.add(listener);
        }
    }

    @Override
    public void unregisterEventListener(@NonNull Object listener) {
        if (listener instanceof org.bukkit.event.Listener) {
            org.bukkit.event.HandlerList.unregisterAll(
                    (org.bukkit.event.Listener) listener);
            eventListeners.remove(listener);
        }
    }

    @Override
    public @NonNull String getAPIVersion() {
        return API_VERSION;
    }

    @Override
    public boolean isDebugMode() {
        return plugin.isDebugMode();
    }

    @Override
    public void setDebugMode(boolean debug) {
        plugin.setDebugMode(debug);
    }
}