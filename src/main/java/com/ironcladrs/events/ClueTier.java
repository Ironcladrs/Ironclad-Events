package com.ironcladrs.events;

/** The lowest clue tier the plugin shares, in the game's order. */
public enum ClueTier
{
    BEGINNER("Beginner"), EASY("Easy"), MEDIUM("Medium"), HARD("Hard"), ELITE("Elite"), MASTER("Master");

    private final String label;
    ClueTier(String label) { this.label = label; }
    @Override public String toString() { return label; }
    /** "hard" → HARD; unknown → BEGINNER, so nothing is filtered by a bad value. */
    public static ClueTier of(String name)
    {
        for (ClueTier tier : values()) { if (tier.name().equalsIgnoreCase(name == null ? "" : name.trim())) { return tier; } }
        return BEGINNER;
    }
}
