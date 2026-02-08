package me.sunmc.dodgeball.arena;

public enum ArenaState {
    WAITING,    // Waiting for players
    STARTING,   // Countdown before game starts
    IN_GAME,    // Game in progress
    ENDING,     // Game ending, showing results
    RESETTING,  // Arena resetting
    DISABLED    // Arena disabled by admin
}