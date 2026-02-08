package me.sunmc.dodgeball.game;

public enum GameMode {
    CLASSIC("Classic", "Last team standing wins"),
    ELIMINATION("Elimination", "Eliminated players don't respawn"),
    INFECTION("Infection", "Hit players join the other team"),
    KING_OF_THE_HILL("King of the Hill", "Control the center area"),
    CAPTURE_THE_FLAG("Capture the Flag", "Capture enemy's flag");

    private final String displayName;
    private final String description;

    GameMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}