package com.ironcladrs.events.sync;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import java.time.Instant;

/** Copy-on-publication snapshot; consumers cannot mutate a running session. */
public final class SessionState
{
    public final boolean linked;
    public final String message;
    public final Instant syncedAt;
    private final JsonObject hello;
    private final JsonObject sync;
    public SessionState(boolean linked, String message, Instant syncedAt, JsonObject hello, JsonObject sync)
    {
        this.linked = linked; this.message = message; this.syncedAt = syncedAt;
        this.hello = hello.deepCopy(); this.sync = capped(sync);
    }
    private static JsonObject capped(JsonObject source)
    {
        JsonObject copy = source.deepCopy(); cap(copy, "events", 30); cap(copy, "claims", 25); cap(copy, "notices", 50); cap(copy, "pbLadders", 100);
        for (JsonElement value : Json.array(copy, "events"))
        {
            if (!value.isJsonObject()) { continue; } JsonObject event = value.getAsJsonObject();
            if (!EventView.supported(event))
            {
                for (String name : new String[]{"tasks", "prechecksOwed", "recommendations", "progress", "nextSteps"}) { event.add(name, new JsonArray()); }
                event.add("match", Json.obj("mode", "none")); event.add("metrics", new JsonObject()); event.add("priority", new JsonObject());
            }
            cap(event, "tasks", 300); cap(event, "links", 8); cap(event, "nextSteps", 3); cap(event, "prechecksOwed", 100);
            JsonObject priority = Json.object(event, "priority"); cap(priority, "targets", 10); if (event.has("priority")) { event.add("priority", priority); }
            cap(event, "recommendations", 50); cap(event, "standings", 5); cap(event, "progress", 100);
        }
        return copy;
    }
    private static void cap(JsonObject object, String name, int limit)
    {
        JsonArray values = Json.array(object, name); JsonArray limited = new JsonArray();
        for (int i = 0; i < Math.min(limit, values.size()); i++) { limited.add(values.get(i)); } object.add(name, limited);
    }
    public static SessionState empty(String message)
    { return new SessionState(false, message, null, new JsonObject(), new JsonObject()); }
    public JsonObject hello() { return hello.deepCopy(); }
    public JsonObject sync() { return sync.deepCopy(); }
    public boolean feature(String name) { return linked && Json.flag(Json.object(hello, "features"), name); }
    public int limit(String name, int fallback, int min, int max)
    { return Json.bounded(Json.object(hello, "limits"), name, fallback, min, max); }
    public String clanName() { return Json.text(hello, "clanName"); }
    public JsonArray events() { return Json.array(sync, "events").deepCopy(); }
    public static boolean inWindow(JsonObject event, Instant at)
    {
        Instant start = Json.time(Json.text(event, "startsAt")); Instant end = Json.time(Json.text(event, "endsAt"));
        return EventView.supported(event) && "live".equals(Json.text(event, "state")) && start != null && end != null && !at.isBefore(start) && at.isBefore(end);
    }
    public boolean hasLiveEvent(Instant at)
    { for (JsonElement event : events()) { if (event.isJsonObject() && inWindow(event.getAsJsonObject(), at)) { return true; } } return false; }
}
