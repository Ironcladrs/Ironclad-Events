package com.ironcladrs.events;

/** The lowest combat task tier the plugin announces, in the game's order. */
public enum CombatTaskTier
{
    EASY("Easy"), MEDIUM("Medium"), HARD("Hard"), ELITE("Elite"), MASTER("Master"), GRANDMASTER("Grandmaster");
    private final String label;
    CombatTaskTier(String label) { this.label = label; }
    @Override public String toString() { return label; }
    public static CombatTaskTier of(String name)
    { for (CombatTaskTier t : values()) { if (t.name().equalsIgnoreCase(name == null ? "" : name.trim())) { return t; } } return EASY; }
}
