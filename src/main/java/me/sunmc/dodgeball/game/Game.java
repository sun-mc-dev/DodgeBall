package me.sunmc.dodgeball.game;

import me.sunmc.dodgeball.DodgeballPlugin;
import me.sunmc.dodgeball.api.events.GameStateChangeEvent;
import me.sunmc.dodgeball.game.enums.GameState;
import me.sunmc.dodgeball.game.enums.GameTeam;
import me.sunmc.dodgeball.storage.GameFileStorage;
import me.sunmc.dodgeball.utility.fastboard.FastBoard;
import me.sunmc.dodgeball.utility.location.LocationHelper;
import me.sunmc.dodgeball.utility.messaging.MessageUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Main game class representing a dodgeball game instance.
 * Modernized for Paper 1.21.1 with Adventure API support.
 */
public class Game {

    private final DodgeballPlugin plugin;
    private final GameFileStorage config;
    private final String worldName;
    private final String gameId;
    private final Set<GameTeam> teams;
    private final List<GamePlayer> players;
    private final Map<UUID, FastBoard> fastBoards;
    private final List<Item> snowballs;
    private boolean enabled;
    private GameState gameState;
    private Location waitingLobbySpawn;
    private int waitingCountdown;

    public Game(@NotNull DodgeballPlugin plugin, @NotNull String gameId, @NotNull String worldName) {
        this.plugin = plugin;
        this.enabled = false;
        this.gameId = gameId;
        this.config = plugin.getPluginConfig();
        this.fastBoards = new HashMap<>();
        this.teams = new HashSet<>();
        this.players = new ArrayList<>();
        this.gameState = GameState.SETUP;
        this.worldName = worldName;
        this.waitingCountdown = this.config.getInt("game.waiting-timer");
        this.snowballs = new ArrayList<>();

        // Create default teams
        this.teams.add(new GameTeam("none", "N/A", NamedTextColor.GRAY, "&7&lN/A", false));
        this.teams.add(new GameTeam("spectator", "Spectator", NamedTextColor.GRAY, "&2&lSpec", false));
    }

    /**
     * Loads teams from configuration section.
     *
     * @param teamsSection Configuration section containing team data
     */
    public void loadTeamsFromConfig(@NotNull ConfigurationSection teamsSection) {
        for (String teamId : teamsSection.getKeys(false)) {
            String displayName = teamsSection.getString(teamId + ".displayName", "DISPLAYNAME_ERROR");
            String prefix = teamsSection.getString(teamId + ".prefix", "PREFIX_ERROR");

            TextColor color;
            try {
                String colorStr = teamsSection.getString(teamId + ".color", "RED");
                color = NamedTextColor.NAMES.value(colorStr.toLowerCase());
                if (color == null) {
                    color = NamedTextColor.RED;
                }
            } catch (Exception e) {
                color = NamedTextColor.RED;
            }

            boolean playable = teamsSection.getBoolean(teamId + ".playable");

            GameTeam team = new GameTeam(teamId, displayName, color, prefix, playable);

            // Load location pair
            try {
                Location pos1 = LocationHelper.parseLocation(teamsSection.getString(teamId + ".locationPair.one", ""));
                Location pos2 = LocationHelper.parseLocation(teamsSection.getString(teamId + ".locationPair.two", ""));

                if (pos1 != null && pos2 != null) {
                    team.getPlayableTeamArea().setPositionOne(pos1);
                    team.getPlayableTeamArea().setPositionTwo(pos2);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load team locations for team " + teamId);
            }

            this.teams.add(team);
        }
    }

    /**
     * Sends a player to this game.
     *
     * @param player Player to add
     */
    public void sendPlayer(@NotNull Player player) {
        UUID uuid = player.getUniqueId();

        // Create game player
        GamePlayer gamePlayer = new GamePlayer(this.gameId, uuid, "none");
        this.players.add(gamePlayer);

        // Create scoreboard
        if (!this.fastBoards.containsKey(uuid)) {
            FastBoard fastBoard = new FastBoard(player);
            String scoreboardTitle = this.config.getString("game.scoreboard.title");
            fastBoard.updateTitle(MessageUtils.legacyToMiniMessage(scoreboardTitle));
            this.fastBoards.put(uuid, fastBoard);
        }

        // Teleport player
        player.teleport(this.waitingLobbySpawn);

        // Reset values
        gamePlayer.resetBukkitValues();
    }

    /**
     * Removes a player from this game.
     *
     * @param player Player to remove
     */
    public void removePlayer(@NotNull Player player) {
        UUID uuid = player.getUniqueId();

        GamePlayer gamePlayer = this.getGamePlayer(player);
        if (gamePlayer != null) {
            this.players.remove(gamePlayer);
            gamePlayer.resetBukkitValues();
        }

        // Clear scoreboard
        if (this.fastBoards.containsKey(uuid)) {
            FastBoard fastBoard = this.fastBoards.get(uuid);
            fastBoard.delete();
            this.fastBoards.remove(uuid);
        }

        // Check if should go back to pre-waiting
        if (this.gameState == GameState.WAITING && this.players.size() < this.config.getInt("game.players-to-start-timer")) {
            this.resetValues();
        }
    }

    /**
     * Updates scoreboards for all players in the game.
     */
    public void updateScoreboard() {
        for (FastBoard board : this.fastBoards.values()) {
            GamePlayer gamePlayer = this.getGamePlayer(board.getPlayer());
            if (gamePlayer == null) {
                continue;
            }

            List<String> lines;

            if (this.gameState.isWaiting()) {
                lines = new ArrayList<>(this.config.getStringList("game.scoreboard.waiting-lines"));
                for (int i = 0; i < lines.size(); i++) {
                    String content = lines.get(i);
                    content = this.applyWaitingLinesReplacements(content);
                    content = MessageUtils.legacyToMiniMessage(content);
                    lines.set(i, content);
                }
            } else {
                lines = new ArrayList<>(this.config.getStringList("game.scoreboard.game-lines"));
                for (int i = 0; i < lines.size(); i++) {
                    String content = lines.get(i);
                    content = this.applyGameLinesReplacements(gamePlayer, content);
                    content = MessageUtils.legacyToMiniMessage(content);
                    lines.set(i, content);
                }
            }

            board.updateLines(lines);
        }
    }

    private String applyWaitingLinesReplacements(@NotNull String input) {
        return input.replace("{players}", String.valueOf(this.players.size()));
    }

    private String applyGameLinesReplacements(@NotNull GamePlayer gamePlayer, @NotNull String input) {
        GameTeam team = gamePlayer.getTeam();
        GameTeam oppositeTeam = gamePlayer.getOppositeTeam();

        input = input.replace("{balls_thrown}", String.valueOf(gamePlayer.getBallsThrown()))
                .replace("{hits}", String.valueOf(gamePlayer.getHits()))
                .replace("{team_left}", String.valueOf(team.getAlivePlayers().size()))
                .replace("{team_prefix}", team.getPrefix())
                .replace("{team_display_name}", team.getDisplayName())
                .replace("{team_color}", team.getColor().toString());

        if (oppositeTeam != null) {
            input = input.replace("{opposite_left}", String.valueOf(oppositeTeam.getAlivePlayers().size()))
                    .replace("{opposite_prefix}", oppositeTeam.getPrefix())
                    .replace("{opposite_display_name}", oppositeTeam.getDisplayName())
                    .replace("{opposite_color}", oppositeTeam.getColor().toString());
        }

        return input;
    }

    // Getters and setters
    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void addTeam(@NotNull GameTeam team) {
        if (this.getTeamById(team.getId()) != null) {
            return;
        }
        this.teams.add(team);
    }

    public void removeTeam(@NotNull String teamId) {
        this.teams.removeIf(team -> team.getId().equals(teamId));
    }

    public void setDelayedGameState(@NotNull GameState gameState, long delay) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.actualSetGameState(gameState), delay);
    }

    private void actualSetGameState(@NotNull GameState gameState) {
        GameStateChangeEvent event = new GameStateChangeEvent(this.gameState, gameState, this);
        this.plugin.getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            this.gameState = gameState;
        }
    }

    public @NotNull GameState getGameState() {
        return this.gameState;
    }

    public void setGameState(@NotNull GameState gameState) {
        this.actualSetGameState(gameState);
    }

    public @Nullable GameTeam getTeamById(@NotNull String id) {
        return this.teams.stream()
                .filter(team -> team.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public @NotNull Set<GameTeam> getTeams() {
        return this.teams;
    }

    public @NotNull List<GameTeam> getPlayableTeams() {
        return this.teams.stream()
                .filter(GameTeam::isPlayable)
                .toList();
    }

    public boolean inGame(@NotNull UUID uuid) {
        return this.players.stream()
                .anyMatch(gamePlayer -> gamePlayer.getUuid().equals(uuid));
    }

    public @Nullable GamePlayer getGamePlayer(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        return this.players.stream()
                .filter(gamePlayer -> gamePlayer.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    public @NotNull List<GamePlayer> getPlayers() {
        return this.players;
    }

    public @Nullable Location getWaitingLobbySpawn() {
        return this.waitingLobbySpawn;
    }

    public void setWaitingLobbySpawn(@NotNull Location waitingLobbySpawn) {
        this.waitingLobbySpawn = waitingLobbySpawn;
    }

    public int getWaitingCountdown() {
        return this.waitingCountdown;
    }

    public void decreaseWaitingCountdown() {
        this.waitingCountdown--;
    }

    public void resetWaitingCountdown() {
        this.waitingCountdown = this.config.getInt("game.waiting-timer");
    }

    public void resetValues() {
        this.resetWaitingCountdown();
        this.setDelayedGameState(GameState.PRE_WAITING, 3);
        this.clearSnowballs();
    }

    public void clearSnowballs() {
        this.snowballs.forEach(Item::remove);
        this.snowballs.clear();
    }

    public @NotNull String getWorldName() {
        return this.worldName;
    }

    public void addSnowball(@NotNull Item item) {
        this.snowballs.add(item);
    }

    public void removeSnowball(@NotNull Item item) {
        this.snowballs.remove(item);
    }

    public @NotNull List<Item> getSnowballs() {
        return this.snowballs;
    }

    public @NotNull DodgeballPlugin getPlugin() {
        return this.plugin;
    }

    public @NotNull String getGameId() {
        return this.gameId;
    }
}