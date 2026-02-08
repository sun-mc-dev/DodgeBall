
package me.sunmc.dodgeball.stats;

import me.sunmc.dodgeball.DodgeBall;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Player statistics - FULLY IMPLEMENTED with file-based storage
 */
public class PlayerStats {

    private final @NonNull UUID playerId;
    private final @NonNull DodgeBall plugin;

    private final AtomicInteger kills = new AtomicInteger(0);
    private final AtomicInteger deaths = new AtomicInteger(0);
    private final AtomicInteger catches = new AtomicInteger(0);
    private final AtomicInteger wins = new AtomicInteger(0);
    private final AtomicInteger losses = new AtomicInteger(0);
    private final AtomicInteger shoot = new AtomicInteger(0);
    private final AtomicInteger gamesPlayed = new AtomicInteger(0);
    private final AtomicLong playtime = new AtomicLong(0);

    public PlayerStats(@NonNull UUID playerId, @NonNull DodgeBall plugin) {
        this.playerId = playerId;
        this.plugin = plugin;
    }

    /**
     * Loads stats from file storage
     */
    public void load() {
        File statsDir = new File(plugin.getDataFolder(), "playerdata");
        if (!statsDir.exists()) {
            statsDir.mkdirs();
        }

        File statsFile = new File(statsDir, playerId.toString() + ".yml");
        if (!statsFile.exists()) {
            return; // New player, no stats yet
        }

        try {
            // Create temporary config for this player's stats
            YamlConfigurationLoader loader =
                    YamlConfigurationLoader.builder()
                            .file(statsFile)
                            .build();

            ConfigurationNode node = loader.load();

            kills.set(node.node("kills").getInt(0));
            deaths.set(node.node("deaths").getInt(0));
            catches.set(node.node("catches").getInt(0));
            wins.set(node.node("wins").getInt(0));
            losses.set(node.node("losses").getInt(0));
            shoot.set(node.node("throws").getInt(0));
            gamesPlayed.set(node.node("games-played").getInt(0));
            playtime.set(node.node("playtime").getLong(0));

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load stats for " + playerId);
            e.printStackTrace();
        }
    }

    /**
     * Saves stats to file storage
     */
    public void save() {
        File statsDir = new File(plugin.getDataFolder(), "playerdata");
        if (!statsDir.exists()) {
            statsDir.mkdirs();
        }

        File statsFile = new File(statsDir, playerId.toString() + ".yml");

        try {
            YamlConfigurationLoader loader =
                    YamlConfigurationLoader.builder()
                            .file(statsFile)
                            .build();

            ConfigurationNode node = loader.createNode();

            node.node("kills").set(kills.get());
            node.node("deaths").set(deaths.get());
            node.node("catches").set(catches.get());
            node.node("wins").set(wins.get());
            node.node("losses").set(losses.get());
            node.node("throws").set(shoot.get());
            node.node("games-played").set(gamesPlayed.get());
            node.node("playtime").set(playtime.get());
            node.node("last-save").set(System.currentTimeMillis());

            loader.save(node);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save stats for " + playerId);
            e.printStackTrace();
        }
    }

    /**
     * Resets all stats to zero
     */
    public void reset() {
        kills.set(0);
        deaths.set(0);
        catches.set(0);
        wins.set(0);
        losses.set(0);
        shoot.set(0);
        gamesPlayed.set(0);
        playtime.set(0);
        save();
    }

    public void incrementKills() { kills.incrementAndGet(); }
    public void incrementDeaths() { deaths.incrementAndGet(); }
    public void incrementCatches() { catches.incrementAndGet(); }
    public void incrementWins() {
        wins.incrementAndGet();
        gamesPlayed.incrementAndGet();
    }
    public void incrementLosses() {
        losses.incrementAndGet();
        gamesPlayed.incrementAndGet();
    }
    public void incrementShoots() { shoot.incrementAndGet(); }
    public void addPlaytime(long milliseconds) { playtime.addAndGet(milliseconds); }

    public @NonNull UUID getPlayerId() { return playerId; }
    public int getKills() { return kills.get(); }
    public int getDeaths() { return deaths.get(); }
    public int getCatches() { return catches.get(); }
    public int getWins() { return wins.get(); }
    public int getLosses() { return losses.get(); }
    public int getShoots() { return shoot.get(); }
    public int getGamesPlayed() { return gamesPlayed.get(); }
    public long getPlaytime() { return playtime.get(); }

    public double getKDRatio() {
        int d = deaths.get();
        return d == 0 ? kills.get() : (double) kills.get() / d;
    }

    public double getWinRate() {
        int total = gamesPlayed.get();
        return total == 0 ? 0 : (double) wins.get() / total * 100;
    }
}