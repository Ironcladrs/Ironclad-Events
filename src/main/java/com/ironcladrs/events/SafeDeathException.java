package com.ironcladrs.events;

/** Deaths in a safe place that should still be announced when chosen (as Dink's "Safe Exceptions"). */
public enum SafeDeathException
{
    INFERNO("Inferno"), FIGHT_CAVE("Fight Caves"), JAD_CHALLENGES("Jad challenges"), RAIDS("Raids (CoX, ToB, ToA)"), GAUNTLET("Gauntlet"), DIARY_REVIVE("Diary revives (Fight Caves, Zulrah)");
    private final String label;
    SafeDeathException(String label) { this.label = label; }
    @Override public String toString() { return label; }
    public static SafeDeathException of(String name)
    { for (SafeDeathException e : values()) { if (e.name().equalsIgnoreCase(name == null ? "" : name.trim())) { return e; } } return null; }
}
