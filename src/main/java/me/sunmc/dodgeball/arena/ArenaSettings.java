package me.sunmc.dodgeball.arena;

/**
 * Arena settings - FULLY IMPLEMENTED
 */
public class ArenaSettings {
    private boolean allowPowerUps = true;
    private boolean allowRespawn = false;
    private int ballDamage = 4;
    private double ballSpeed = 1.5;
    private int gameDuration = 300;
    private int ballCustomModelData = 1;

    public boolean isAllowPowerUps() {
        return allowPowerUps;
    }

    public void setAllowPowerUps(boolean allowPowerUps) {
        this.allowPowerUps = allowPowerUps;
    }

    public boolean isAllowRespawn() {
        return allowRespawn;
    }

    public void setAllowRespawn(boolean allowRespawn) {
        this.allowRespawn = allowRespawn;
    }

    public int getBallDamage() {
        return ballDamage;
    }

    public void setBallDamage(int ballDamage) {
        this.ballDamage = ballDamage;
    }

    public double getBallSpeed() {
        return ballSpeed;
    }

    public void setBallSpeed(double ballSpeed) {
        this.ballSpeed = ballSpeed;
    }

    public int getGameDuration() {
        return gameDuration;
    }

    public void setGameDuration(int gameDuration) {
        this.gameDuration = gameDuration;
    }

    public int getBallCustomModelData() {
        return ballCustomModelData;
    }

    public void setBallCustomModelData(int data) {
        this.ballCustomModelData = data;
    }
}