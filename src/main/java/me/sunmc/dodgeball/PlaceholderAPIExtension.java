package me.sunmc.dodgeball;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.sunmc.dodgeball.game.Game;
import me.sunmc.dodgeball.game.GameHelper;
import me.sunmc.dodgeball.game.enums.GameTeam;
import me.sunmc.dodgeball.storage.user.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * PlaceholderAPI expansion for Dodgeball plugin.
 * Updated for Paper 1.21.11.
 */
public class PlaceholderAPIExtension extends PlaceholderExpansion {

    private static final String INVALID = "[invalid]";
    private final DodgeballPlugin plugin;

    public PlaceholderAPIExtension(@NotNull DodgeballPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dodgeball";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SunMC";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@NotNull OfflinePlayer player, @NotNull String params) {
        User user = this.plugin.getUserStorage().getCachedUser(player.getUniqueId());
        if (user == null) {
            return "";
        }

        // Players remaining in each team (format: "Team1: 5 - Team2: 3")
        if (params.equals("playersleft")) {
            Player target = Bukkit.getPlayer(player.getUniqueId());
            if (target == null) {
                return INVALID;
            }

            Game game = GameHelper.getGameFromPlayer(target);
            if (game == null) {
                return INVALID;
            }

            List<GameTeam> playableTeams = game.getPlayableTeams();
            if (playableTeams.size() != 2) {
                return INVALID;
            }

            GameTeam teamOne = playableTeams.get(0);
            GameTeam teamTwo = playableTeams.get(1);

            return teamOne.getDisplayName() + ": " + teamOne.getAlivePlayers().size() +
                    " - " + teamTwo.getDisplayName() + ": " + teamTwo.getAlivePlayers().size();
        }

        // Player level
        if (params.equals("level")) {
            return String.valueOf(user.getLevel());
        }

        // Player coins
        if (params.equals("coins")) {
            return String.valueOf(user.getCoins());
        }

        // Lifetime kills
        if (params.equals("lifetime_kills")) {
            return String.valueOf(user.getLifetimeKills());
        }

        // Lifetime deaths
        if (params.equals("lifetime_deaths")) {
            return String.valueOf(user.getLifetimeDeaths());
        }

        return null;
    }
}