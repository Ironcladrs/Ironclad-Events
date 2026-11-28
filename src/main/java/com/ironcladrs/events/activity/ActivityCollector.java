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
    private static final Pattern COMBAT_TASK = Pattern.compile("Congratulations, you've completed an? (easy|medium|hard|elite|master|grandmaster) combat task: (.+?)(?: \\([0-9]+ points?\\))?\\.?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIARY = Pattern.compile("Congratulations! You have completed all of the (easy|medium|hard|elite) tasks in the (.+?) area", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLUE = Pattern.compile("You have completed ([0-9,]+) (beginner|easy|medium|hard|elite|master) Treasure Trails?\\.", Pattern.CASE_INSENSITIVE);
    @Inject private Client client;
    @Inject private Consent consent;
    @Inject private Outbox outbox;
    @Inject private com.ironcladrs.events.IroncladEventsConfig config;
    @Inject private com.ironcladrs.events.sync.SyncService sync;
    private com.ironcladrs.events.filters.Filters filters() { return new com.ironcladrs.events.filters.Filters(config, sync.state()); }
    /** The level the rules count: real, or virtual (100-126 from XP) when that setting is on. */
    private int levelOf(Skill skill, boolean virtual)
    { return virtual ? Math.min(126, Experience.getLevelForXp(client.getSkillExperience(skill))) : client.getRealSkillLevel(skill); }
    /** Total level always counts real levels, as the game shows it. */
    private int realTotal()
    { int sum = 0; for (Skill skill : Skill.values()) { if (skill != Skill.OVERALL) { sum += client.getRealSkillLevel(skill); } } return sum; }
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
        {
            // "...would have been followed" is a pet the player already owns.
            boolean duplicate = text.contains("would have been followed");
            if (!duplicate || filters().petDuplicates) { add("pets", "pet:" + client.getTickCount(), Json.obj("kind", "pet")); }
        }
        Matcher task = COMBAT_TASK.matcher(text);
        if (task.find())
        {
            String tier = task.group(1).toLowerCase(Locale.ROOT), name = task.group(2).trim();
            if (filters().announcesCombatTask(tier)) { add("combatTasks", "ca:" + name.toLowerCase(Locale.ROOT), Json.obj("kind", "ca", "tier", tier, "name", name)); }
        }
        Matcher diary = DIARY.matcher(text);
        if (diary.find())
        {
            String tier = diary.group(1).toLowerCase(Locale.ROOT), area = diary.group(2).trim();
            if (filters().announcesDiary(tier)) { add("diaries", "diary:" + tier + ":" + area.toLowerCase(Locale.ROOT), Json.obj("kind", "diary", "tier", tier, "area", area)); }
        }
        Matcher clue = CLUE.matcher(text);
        if (clue.find())
        {
            try { int count = Integer.parseInt(clue.group(1).replace(",", "")); String tier = clue.group(2).toLowerCase(Locale.ROOT);
                if (!filters().sharesClue(tier)) { return; }
                add("clues", "clue:" + tier + ":" + count, Json.obj("kind", "clue", "tier", tier, "count", count)); }
            catch (NumberFormatException ignored) { /* Invalid game line. */ }
        }
    }
    @Subscribe public void onStatChanged(StatChanged event)
    {
        if (!consent.allows("milestones") || levels.isEmpty() || event.getSkill() == Skill.OVERALL) { return; }
        com.ironcladrs.events.filters.Filters rules = filters();
        Skill skill = event.getSkill(); int level = levelOf(skill, rules.virtualLevels); Integer old = levels.put(skill, level);
        if (old == null || level <= old) { return; }
        // Every level crossed counts, so a multi-level jump still announces the right ones.
        for (int l = old + 1; l <= level; l++)
        { if (rules.announcesLevel(l)) { add("milestones", skill.name() + ":" + l, Json.obj("kind", "level", "skill", skill.name(), "level", l)); } }
        int current = realTotal();
        if (rules.announcesTotal(total, current))
        { int milestone = rules.totalMilestone(current); add("milestones", "total:" + milestone, Json.obj("kind", "total", "level", milestone)); }
        total = current;
    }
    public void tick(Instant now)
    {
        if (client.getTickCount() < readyTick) { return; }
        if (consent.allows("milestones") && levels.isEmpty())
        {
            boolean virtual = filters().virtualLevels;
            for (Skill skill : Skill.values()) { if (skill != Skill.OVERALL) { levels.put(skill, levelOf(skill, virtual)); } }
            total = realTotal();
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
