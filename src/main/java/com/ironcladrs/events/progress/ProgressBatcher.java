package com.ironcladrs.events.progress;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.queue.*;
import com.ironcladrs.events.sync.*;
import java.time.Instant;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/** Reports current totals, never event scores. Rate limiting includes immediate KC reports. */
@Singleton
public class ProgressBatcher
{
    @Inject private Client client;
    @Inject private Consent consent;
    @Inject private SyncService sync;
    @Inject private Outbox outbox;
    @Inject private XpCollector xp;
    @Inject private KillCountCollector kc;
    private Instant next = Instant.EPOCH, observedAt;
    private final Map<Long, Long> activeSeconds = new HashMap<>();
    @Subscribe public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE) { return; }
        kc.accept(Text.removeTags(event.getMessage()), client.getTickCount());

    }
    public void tick(Instant now)
    {
        if (!consent.allows("progress")) { activeSeconds.clear(); observedAt = null; return; }
        Set<Long> liveIds = new HashSet<>();
        for (JsonElement value : sync.state().events())
        { if (value.isJsonObject() && SessionState.inWindow(value.getAsJsonObject(), now)) { liveIds.add(Json.number(value.getAsJsonObject(), "id", -1)); } }
        long elapsed = observedAt == null ? 0 : Math.max(0, Math.min(2, java.time.Duration.between(observedAt, now).getSeconds())); observedAt = now;
        activeSeconds.keySet().retainAll(liveIds); for (long id : liveIds) { activeSeconds.merge(id, elapsed, Long::sum); }
        if (now.isBefore(next)) { return; }
        Set<String> skills = tracked("xp", now), bosses = tracked("kc", now);
        JsonArray xpRows = new JsonArray(), kcRows = new JsonArray();
        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL || !skills.contains(Json.norm(skill.name()))) { continue; }
            Integer value = xp.get(skill); xpRows.add(Json.obj("skill", skill.name(), "xp", value == null ? client.getSkillExperience(skill) : value));
        }
        for (String boss : bosses) { KillCountCollector.Count count = kc.get(boss); if (count != null) { kcRows.add(Json.obj("name", count.name, "count", count.count)); } }
        JsonArray activity = new JsonArray(); activeSeconds.forEach((id, seconds) -> { if (seconds > 0) { activity.add(Json.obj("eventId", id, "seconds", Math.min(900, seconds))); } });
        if (xpRows.size() == 0 && kcRows.size() == 0 && activity.size() == 0) { return; }
        if (outbox.submit("progress", "/progress", Json.obj("at", now, "xp", xpRows, "kc", kcRows, "activeSeconds", activity), null, ignored -> {}))
        { activeSeconds.replaceAll((id, seconds) -> 0L); next = now.plusSeconds(sync.state().limit("progressEverySec", 300, 20, 3600)); }
    }
    private Set<String> tracked(String key, Instant at)
    {
        Set<String> result = new HashSet<>();
        for (JsonElement value : sync.state().events())
        { if (value.isJsonObject() && SessionState.inWindow(value.getAsJsonObject(), at))
        { for (JsonElement metric : Json.array(Json.object(value.getAsJsonObject(), "metrics"), key)) { if (metric.isJsonPrimitive()) { result.add(Json.norm(metric.getAsString())); } } } }
        return result;
    }
    public void reset() { next = Instant.EPOCH; observedAt = null; activeSeconds.clear(); }
}
