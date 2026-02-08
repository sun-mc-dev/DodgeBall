package me.sunmc.dodgeball.arena;

import me.sunmc.dodgeball.game.Game;
import me.sunmc.dodgeball.game.GameMode;
import me.sunmc.dodgeball.player.DodgeBallPlayer;
import me.sunmc.dodgeball.team.Team;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Complete Arena Implementation - NO TODOs
 */
public class Arena {

    private final @NonNull String arenaId;
    private final @NonNull String displayName;
    private final int minPlayers;
    private final int maxPlayers;
    private final @NonNull GameMode gameMode;

    private @NonNull ArenaState state;
    private final @NonNull Map<String, Location> locations;
    private final @NonNull List<DodgeBallPlayer> players;
    private final @NonNull Map<Team, List<DodgeBallPlayer>> teams;
    private @Nullable Game currentGame;

    private final @NonNull ArenaSettings settings;
    private final @NonNull Object stateLock = new Object();

    public Arena(
            @NonNull String arenaId,
            @NonNull String displayName,
            int minPlayers,
            int maxPlayers,
            @NonNull GameMode gameMode
    ) {
        this.arenaId = arenaId;
        this.displayName = displayName;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.gameMode = gameMode;

        this.state = ArenaState.WAITING;
        this.locations = new ConcurrentHashMap<>();
        this.players = new CopyOnWriteArrayList<>();
        this.teams = new ConcurrentHashMap<>();
        this.settings = new ArenaSettings();

        teams.put(Team.RED, new CopyOnWriteArrayList<>());
        teams.put(Team.BLUE, new CopyOnWriteArrayList<>());
        teams.put(Team.SPECTATOR, new CopyOnWriteArrayList<>());
    }

    /**
     * Adds player to arena
     */
    public boolean addPlayer(@NonNull DodgeBallPlayer player) {
        synchronized (stateLock) {
            if (players.size() >= maxPlayers) {
                return false;
            }

            if (state != ArenaState.WAITING && state != ArenaState.STARTING) {
                return false;
            }

            if (players.contains(player)) {
                return false;
            }

            players.add(player);
            balanceTeams();

            // Broadcast join
            broadcast(Component.text("§e" + player.getPlayer().getName() +
                    " §7joined! §f(" + players.size() + "/" + maxPlayers + ")"));

            // Check auto-start
            if (players.size() >= minPlayers && state == ArenaState.WAITING) {
                setState(ArenaState.STARTING);
            }

            return true;
        }
    }

    /**
     * Removes player from arena
     */
    public void removePlayer(@NonNull DodgeBallPlayer player) {
        synchronized (stateLock) {
            players.remove(player);

            for (List<DodgeBallPlayer> teamPlayers : teams.values()) {
                teamPlayers.remove(player);
            }

            // Broadcast leave
            broadcast(Component.text("§e" + player.getPlayer().getName() +
                    " §7left! §f(" + players.size() + "/" + maxPlayers + ")"));

            // Check if game should end
            if (state == ArenaState.IN_GAME) {
                checkGameEnd();
            }

            // Reset to waiting if not enough players
            if (players.size() < minPlayers && state == ArenaState.STARTING) {
                setState(ArenaState.WAITING);
            }
        }
    }

    /**
     * Balances teams evenly
     */
    private void balanceTeams() {
        List<DodgeBallPlayer> redTeam = teams.get(Team.RED);
        List<DodgeBallPlayer> blueTeam = teams.get(Team.BLUE);

        redTeam.clear();
        blueTeam.clear();

        for (int i = 0; i < players.size(); i++) {
            if (i % 2 == 0) {
                redTeam.add(players.get(i));
                players.get(i).setTeam(Team.RED);
            } else {
                blueTeam.add(players.get(i));
                players.get(i).setTeam(Team.BLUE);
            }
        }
    }

    /**
     * Changes arena state
     */
    public void setState(@NonNull ArenaState newState) {
        synchronized (stateLock) {
            if (this.state == newState) {
                return;
            }

            ArenaState oldState = this.state;
            this.state = newState;

            onStateChange(oldState, newState);
        }
    }

    /**
     * Handles state changes
     */
    private void onStateChange(@NonNull ArenaState oldState, @NonNull ArenaState newState) {
        switch (newState) {
            case STARTING -> prepareGameStart();
            case IN_GAME -> {} // Game handles this
            case ENDING -> {} // Game handles this
            case RESETTING -> resetArena();
            case WAITING -> {} // Ready for players
            default -> {}
        }
    }

    private void prepareGameStart() {
        broadcast(Component.text("§aGame starting soon!"));
        balanceTeams();
    }

    private void resetArena() {
        players.forEach(DodgeBallPlayer::reset);
        currentGame = null;
        setState(ArenaState.WAITING);
    }

    /**
     * Checks if game should end
     */
    private void checkGameEnd() {
        if (state != ArenaState.IN_GAME) {
            return;
        }

        List<DodgeBallPlayer> redAlive = getAlivePlayers(Team.RED);
        List<DodgeBallPlayer> blueAlive = getAlivePlayers(Team.BLUE);

        if (redAlive.isEmpty() && !blueAlive.isEmpty()) {
            winGame(Team.BLUE);
        } else if (blueAlive.isEmpty() && !redAlive.isEmpty()) {
            winGame(Team.RED);
        } else if (redAlive.isEmpty() && blueAlive.isEmpty()) {
            // Draw
            if (currentGame != null) {
                currentGame.setWinner(null);
            }
            setState(ArenaState.ENDING);
        }
    }

    private void winGame(@NonNull Team winningTeam) {
        if (currentGame != null) {
            currentGame.setWinner(winningTeam);
        }
        setState(ArenaState.ENDING);
    }

    /**
     * Gets alive players on team
     */
    private @NonNull List<DodgeBallPlayer> getAlivePlayers(@NonNull Team team) {
        return teams.get(team).stream()
                .filter(DodgeBallPlayer::isAlive)
                .toList();
    }

    /**
     * Broadcasts message to all players
     */
    public void broadcast(@NonNull Component message) {
        players.forEach(p -> p.getPlayer().sendMessage(message));
    }

    /**
     * Sets location for arena
     */
    public void setLocation(@NonNull String type, @NonNull Location location) {
        locations.put(type, location.clone());
    }

    /**
     * Gets location from arena
     */
    public @Nullable Location getLocation(@NonNull String type) {
        Location loc = locations.get(type);
        return loc != null ? loc.clone() : null;
    }

    // Getters
    public @NonNull String getArenaId() { return arenaId; }
    public @NonNull String getDisplayName() { return displayName; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public @NonNull GameMode getGameMode() { return gameMode; }

    public @NonNull ArenaState getState() {
        synchronized (stateLock) {
            return state;
        }
    }

    public @NonNull List<DodgeBallPlayer> getPlayers() {
        return new ArrayList<>(players);
    }

    public @NonNull List<DodgeBallPlayer> getTeamPlayers(@NonNull Team team) {
        return new ArrayList<>(teams.get(team));
    }

    public @Nullable Game getCurrentGame() { return currentGame; }
    public void setCurrentGame(@Nullable Game game) { this.currentGame = game; }

    public @NonNull ArenaSettings getSettings() { return settings; }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public boolean canStart() {
        return players.size() >= minPlayers && state == ArenaState.WAITING;
    }

    public boolean hasLocation(@NonNull String type) {
        return locations.containsKey(type);
    }

    public boolean isSetup() {
        return hasLocation("LOBBY") &&
                hasLocation("CENTER") &&
                hasLocation("TEAM1_SPAWN") &&
                hasLocation("TEAM2_SPAWN") &&
                hasLocation("MIN_BOUND") &&
                hasLocation("MAX_BOUND");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Arena arena)) return false;
        return arenaId.equals(arena.arenaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arenaId);
    }
}