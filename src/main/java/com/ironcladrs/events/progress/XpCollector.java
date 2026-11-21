package com.ironcladrs.events.progress;

import java.util.EnumMap;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import net.runelite.client.eventbus.Subscribe;

/** XP is read from the local player; only tracked skills are selected for upload. */
@Singleton
public class XpCollector
{
    private final Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
    @Subscribe public void onStatChanged(StatChanged event) { xp.put(event.getSkill(), event.getXp()); }
    public Integer get(Skill skill) { return xp.get(skill); }
    public void reset() { xp.clear(); }
}
