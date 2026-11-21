package com.ironcladrs.events.activity;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.queue.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/** Only own game messages and real level changes. Login levels never create achievements. */
@Singleton
public class ActivityCollector
{
    private static final Pattern CLUE = Pattern.compile("You have completed ([0-9,]+) (beginner|easy|medium|hard|elite|master) Treasure Trails?\\.", Pattern.CASE_INSENSITIVE);
    @Inject private Client client;
    @Inject private Consent consent;
    @Inject private Outbox outbox;
    private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
    private final Map<String, JsonArray> pending = new LinkedHashMap<>();
    private final LinkedHashSet<String> seen = new LinkedHashSet<>();
    private int total, readyTick;
    private Instant next = Instant.EPOCH;
    private void add(String feature, String signature, JsonObject event)
    {
        if (!consent.allows(feature) || !seen.add(signature)) { return; }
        while (seen.size() > 200) { seen.remove(seen.iterator().next()); }
        JsonArray rows = pending.computeIfAbsent(feature, ignored -> new JsonArray());
        if (rows.size() < 100) { event.addProperty("at", Instant.now().toString()); rows.add(event); }
    }
    @Subscribe public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM) { return; }
        String text = Text.removeTags(event.getMessage());
        if (text.startsWith("You have a funny feeling like you") || text.startsWith("You feel something weird sneaking"))
        { add("pets", "pet:" + client.getTickCount(), Json.obj("kind", "pet")); }
        Matcher clue = CLUE.matcher(text);
        if (clue.find())
        {
            try { int count = Integer.parseInt(clue.group(1).replace(",", "")); String tier = clue.group(2).toLowerCase(Locale.ROOT);
                add("clues", "clue:" + tier + ":" + count, Json.obj("kind", "clue", "tier", tier, "count", count)); }
            catch (NumberFormatException ignored) { /* Invalid game line. */ }
        }
    }
    @Subscribe public void onStatChanged(StatChanged event)
    {
        if (!consent.allows("milestones") || levels.isEmpty() || event.getSkill() == Skill.OVERALL) { return; }
        Skill skill = event.getSkill(); int level = client.getRealSkillLevel(skill); Integer old = levels.put(skill, level);
        if (old == null || level <= old) { return; }
        if (old < 99 && level >= 99) { add("milestones", skill.name() + ":99", Json.obj("kind", "level", "skill", skill.name(), "level", 99)); }
        int current = levels.values().stream().mapToInt(Integer::intValue).sum();
        if (current / 100 > total / 100 && current >= 500)
        { int milestone = current / 100 * 100; add("milestones", "total:" + milestone, Json.obj("kind", "total", "level", milestone)); }
        total = current;
    }
    public void tick(Instant now)
    {
        if (client.getTickCount() < readyTick) { return; }
        if (consent.allows("milestones") && levels.isEmpty())
        {
            for (Skill skill : Skill.values()) { if (skill != Skill.OVERALL) { levels.put(skill, client.getRealSkillLevel(skill)); } }
            total = levels.values().stream().mapToInt(Integer::intValue).sum();
        }
        if (now.isBefore(next)) { return; }
        for (String feature : new ArrayList<>(pending.keySet()))
        {
            if (!consent.allows(feature)) { pending.remove(feature); continue; }
            JsonArray rows = pending.get(feature), batch = new JsonArray(), rest = new JsonArray();
            for (int i = 0; i < rows.size(); i++) { (i < 20 ? batch : rest).add(rows.get(i)); }
            if (outbox.submit(feature, "/activity", Json.obj("events", batch), null, ignored -> {}))
            { if (rest.size() == 0) { pending.remove(feature); } else { pending.put(feature, rest); } }
        }
        next = now.plusSeconds(3);
    }
    public void reset() { levels.clear(); pending.clear(); seen.clear(); total = 0; readyTick = client.getTickCount() + 2; next = Instant.EPOCH; }
}
