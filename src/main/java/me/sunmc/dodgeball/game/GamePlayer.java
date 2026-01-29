package me.sunmc.dodgeball.game;

import me.sunmc.dodgeball.DodgeballPlugin;
import me.sunmc.dodgeball.game.enums.GameTeam;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores game-specific player data.
 * Updated for Paper 1.21.1.
 */
public class GamePlayer {

    private final DodgeballPlugin plugin;
    private final String gameId;
    private final UUID uuid;
    private String teamId;
    private int ballsThrown;
    private int hits;

    public GamePlayer(@NotNull String gameId, @NotNull UUID uuid, @NotNull String teamId) {
        this.gameId = gameId;
        this.plugin = DodgeballPlugin.getInstance();
        this.uuid = uuid;
        this.teamId = teamId;
        this.ballsThrown = 0;
        this.hits = 0;
    }

    /**
     * Gets the player's current team.
     *
     * @return Current team
     */
    public @NotNull GameTeam getTeam() {
        return Objects.requireNonNull(this.getGame().getTeamById(this.teamId));
    }

    /**
     * Gets the opposite team.
     *
     * @return Opposite team or null
     */
    public @Nullable GameTeam getOppositeTeam() {
        List<GameTeam> playableTeams = this.getGame().getPlayableTeams();

        for (GameTeam team : playableTeams) {
            if (!team.getId().equals(this.getTeam().getId())) {
                return team;
            }
        }

        return null;
    }

    /**
     * Resets player to default state.
     */
    public void resetBukkitValues() {
        Player player = this.toPlayer();
        if (player == null) {
            return;
        }

        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setLevel(0);
        player.setExp(0);
        player.setHealthScale(20.0);
        player.setFoodLevel(20);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed(0.1f);
        player.setWalkSpeed(0.2f);
        player.getInventory().clear();

        // Remove all potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    public void setTeamId(@NotNull String teamId) {
        this.teamId = teamId;
    }

    public int getBallsThrown() {
        return this.ballsThrown;
    }

    public void incrementBallsThrown() {
        this.ballsThrown++;
    }

    public int getHits() {
        return this.hits;
    }

    public void incrementHits() {
        this.hits++;
    }

    public @NotNull UUID getUuid() {
        return this.uuid;
    }

    public @NotNull Game getGame() {
        return this.plugin.getGameById(this.gameId);
    }

    public @Nullable Player toPlayer() {
        return Bukkit.getPlayer(this.uuid);
    }
}