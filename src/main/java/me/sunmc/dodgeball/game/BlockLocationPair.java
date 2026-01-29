package me.sunmc.dodgeball.game;

import org.bukkit.Location;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Stores two locations together. Used to create cuboids for playable areas in the game.
 */
public class BlockLocationPair {

    private Location positionOne;
    private Location positionTwo;

    public boolean isBothPositionSet() {
        return this.isPositionOneSet() && this.isPositionTwoSet();
    }

    public boolean isPositionOneSet() {
        return this.positionOne != null;
    }

    public boolean isPositionTwoSet() {
        return this.positionTwo != null;
    }

    public @Nullable Location getPositionOne() {
        return this.positionOne;
    }

    public void setPositionOne(@NonNull Location positionOne) {
        this.positionOne = new Location(positionOne.getWorld(), positionOne.getBlockX(), positionOne.getBlockY(), positionOne.getBlockZ());
    }

    public @Nullable Location getPositionTwo() {
        return this.positionTwo;
    }

    public void setPositionTwo(@NonNull Location positionTwo) {
        this.positionTwo = new Location(positionTwo.getWorld(), positionTwo.getBlockX(), positionTwo.getBlockY(), positionTwo.getBlockZ());
    }
}