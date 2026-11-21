package com.ironcladrs.events.account;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.ca.CombatAchievementReader;
import com.ironcladrs.events.queue.*;
import java.time.Instant;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/** Account parts share the 12/hour budget. Changes coalesce without ever sending gear or location. */
@Singleton
public class AccountSnapshot
{
    @Inject private Client client;
    @Inject private Consent consent;
    @Inject private Outbox outbox;
    @Inject private CombatAchievementReader ca;
    private JsonObject pending = new JsonObject(), last = new JsonObject();
    private final java.util.Deque<Instant> sent = new ArrayDeque<>();
    private boolean dirty = true;
    private Instant collectAt = Instant.EPOCH;
    private int readyTick;
    private final Map<Skill, Integer> observedLevels = new EnumMap<>(Skill.class);
    public void part(String name, JsonObject value)
    {
        if (!consent.accountPart(name)) { return; }
        if ("collectionLog".equals(name) && !Json.flag(value, "full") && pending.has(name))
        {
            JsonObject existing = Json.object(pending, name); JsonObject merged = Json.object(existing, "items");
            Json.object(value, "items").entrySet().forEach(entry -> merged.add(entry.getKey(), entry.getValue()));
            existing.add("items", merged); if (Json.flag(value, "newItem")) { existing.addProperty("newItem", true); } pending.add(name, existing);
        }
        else if (!value.equals(last.get(name))) { pending.add(name, value.deepCopy()); }
    }
    @Subscribe public void onStatChanged(StatChanged event)
    { if (consent.allows("account") && event.getSkill() != Skill.OVERALL)
      { int level = client.getRealSkillLevel(event.getSkill()); Integer old = observedLevels.put(event.getSkill(), level); if (old == null || old != level) { dirty = true; } } }
    @Subscribe public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE || !consent.allows("account")) { return; }
        String text = Text.removeTags(event.getMessage());
        if (text.contains("combat task:") || text.contains("Quest complete") || text.contains("Congratulations, you've completed") || text.contains("diary")) { dirty = true; collectAt = Instant.now().plusSeconds(2); }
    }
    public void tick(Instant now)
    {
        if (!consent.allows("account")) { pending = new JsonObject(); dirty = true; return; }
        if (client.getTickCount() < readyTick) { return; }
        if (dirty && !now.isBefore(collectAt)) { collect(); dirty = false; collectAt = now.plusSeconds(30); }
        while (!sent.isEmpty() && !now.isBefore(sent.peek().plusSeconds(3600))) { sent.remove(); }
        // A part switched off after it was collected never leaves.
        for (String name : new java.util.ArrayList<>(pending.keySet())) { if (!consent.accountPart(name)) { pending.remove(name); } }
        if (pending.size() == 0 || sent.size() >= 12) { return; }
        JsonObject payload = pending.deepCopy(); payload.addProperty("at", now.toString());
        if (outbox.submit("account", "/account", payload, null, result -> { if (!result.success()) { dirty = true; } }))
        { pending.entrySet().forEach(entry -> last.add(entry.getKey(), entry.getValue().deepCopy())); pending = new JsonObject(); sent.add(now); }
    }
    private void collect()
    {
        JsonObject levels = new JsonObject(), quests = new JsonObject(), diaries = new JsonObject();
        for (Skill skill : Skill.values()) { if (skill != Skill.OVERALL) { int level = client.getRealSkillLevel(skill); levels.addProperty(skill.name(), level); observedLevels.put(skill, level); } }
        for (Quest quest : Quest.values()) { quests.add(Integer.toString(quest.getId()), Json.obj("name", quest.getName(), "state", quest.getState(client).name())); }
        // Diary completion flags are sufficient for eligibility; no diary UI or server action is triggered.
        diary(diaries, "Ardougne", VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE, VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE, VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE, VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE);
        diary(diaries, "Falador", VarbitID.FALADOR_DIARY_EASY_COMPLETE, VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE, VarbitID.FALADOR_DIARY_HARD_COMPLETE, VarbitID.FALADOR_DIARY_ELITE_COMPLETE);
        diary(diaries, "Fremennik", VarbitID.FREMENNIK_DIARY_EASY_COMPLETE, VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE, VarbitID.FREMENNIK_DIARY_HARD_COMPLETE, VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE);
        diary(diaries, "Kandarin", VarbitID.KANDARIN_DIARY_EASY_COMPLETE, VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE, VarbitID.KANDARIN_DIARY_HARD_COMPLETE, VarbitID.KANDARIN_DIARY_ELITE_COMPLETE);
        diary(diaries, "Desert", VarbitID.DESERT_DIARY_EASY_COMPLETE, VarbitID.DESERT_DIARY_MEDIUM_COMPLETE, VarbitID.DESERT_DIARY_HARD_COMPLETE, VarbitID.DESERT_DIARY_ELITE_COMPLETE);
        diary(diaries, "Lumbridge", VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE, VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE, VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE, VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE);
        diary(diaries, "Morytania", VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE, VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE);
        diary(diaries, "Varrock", VarbitID.VARROCK_DIARY_EASY_COMPLETE, VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE, VarbitID.VARROCK_DIARY_HARD_COMPLETE, VarbitID.VARROCK_DIARY_ELITE_COMPLETE);
        diary(diaries, "Wilderness", VarbitID.WILDERNESS_DIARY_EASY_COMPLETE, VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE, VarbitID.WILDERNESS_DIARY_HARD_COMPLETE, VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE);
        diary(diaries, "Western Provinces", VarbitID.WESTERN_DIARY_EASY_COMPLETE, VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE, VarbitID.WESTERN_DIARY_HARD_COMPLETE, VarbitID.WESTERN_DIARY_ELITE_COMPLETE);
        diary(diaries, "Kourend", VarbitID.KOUREND_DIARY_EASY_COMPLETE, VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE, VarbitID.KOUREND_DIARY_HARD_COMPLETE, VarbitID.KOUREND_DIARY_ELITE_COMPLETE);
        diary(diaries, "Karamja", net.runelite.api.Varbits.DIARY_KARAMJA_EASY, net.runelite.api.Varbits.DIARY_KARAMJA_MEDIUM, net.runelite.api.Varbits.DIARY_KARAMJA_HARD, VarbitID.KARAMJA_DIARY_ELITE_COMPLETE);
        part("levels", levels); part("quests", quests); part("diaries", diaries);
        JsonObject achievements = ca.capture();
        if (Json.object(achievements, "tiers").size() == 6) { part("combatAchievements", achievements); }
        JsonObject type = Json.obj("ironman", client.getVarbitValue(VarbitID.IRONMAN), "groupSize", client.getVarbitValue(VarbitID.GIM_GROUPSIZE), "hardcoreMain", client.getVarbitValue(VarbitID.HC_MAIN));
        part("accountType", type);
    }
    private void diary(JsonObject result, String name, int... varbits)
    { JsonArray tiers = new JsonArray(); for (int varbit : varbits) { tiers.add(client.getVarbitValue(varbit) != 0); } result.add(name, tiers); }
    public void reset() { pending = new JsonObject(); last = new JsonObject(); sent.clear(); observedLevels.clear(); dirty = true; collectAt = Instant.EPOCH; readyTick = client.getTickCount() + 2; }
}
