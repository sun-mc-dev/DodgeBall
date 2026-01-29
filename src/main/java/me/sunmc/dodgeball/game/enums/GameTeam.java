package me.sunmc.dodgeball.game.enums;

import me.sunmc.dodgeball.game.BlockLocationPair;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a team in the dodgeball game.
 * Modernized with Adventure API TextColor support.
 */
public class GameTeam {

    private final String id;
    private final String displayName;
    private final String prefix;
    private final TextColor color;
    private final BlockLocationPair playableTeamArea;
    private final boolean playable;
    private final List<UUID> alivePlayers;

    public GameTeam(@NotNull String id, @NotNull String displayName, @NotNull TextColor color,
                    @NotNull String prefix, boolean playable) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.prefix = prefix;
        this.playableTeamArea = new BlockLocationPair();
        this.playable = playable;
        this.alivePlayers = new ArrayList<>();
    }

    public @NotNull String getId() {
        return this.id;
    }

    public @NotNull String getDisplayName() {
        return this.displayName;
    }

    public @NotNull TextColor getColor() {
        return this.color;
    }

    public @NotNull String getPrefix() {
        return this.prefix;
    }

    public @NotNull BlockLocationPair getPlayableTeamArea() {
        return this.playableTeamArea;
    }

    public boolean isPlayable() {
        return this.playable;
    }

    public @NotNull List<UUID> getAlivePlayers() {
        return this.alivePlayers;
    }

    public void addAlivePlayer(@NotNull UUID uuid) {
        this.alivePlayers.add(uuid);
    }

    public void removeAlivePlayer(@NotNull UUID uuid) {
        this.alivePlayers.remove(uuid);
    }
}