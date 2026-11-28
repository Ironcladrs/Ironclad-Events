package com.ironcladrs.events;

/** The lowest achievement diary tier the plugin announces. */
public enum DiaryTier
{
    EASY("Easy"), MEDIUM("Medium"), HARD("Hard"), ELITE("Elite");
    private final String label;
    DiaryTier(String label) { this.label = label; }
    @Override public String toString() { return label; }
    public static DiaryTier of(String name)
    { for (DiaryTier t : values()) { if (t.name().equalsIgnoreCase(name == null ? "" : name.trim())) { return t; } } return EASY; }
}
