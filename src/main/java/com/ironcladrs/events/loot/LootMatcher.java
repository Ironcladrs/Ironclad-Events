package com.ironcladrs.events.loot;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.sync.SessionState;
import java.time.Instant;

/** Event matching never evaluates game rules; the server remains the authority. */
public final class LootMatcher
{
    private LootMatcher() {}
    public static boolean matches(JsonArray events, long wallMinGp, String source, JsonArray items, Instant at)
    {
        long value = 0;
        for (JsonElement item : items)
        {
            JsonObject i = item.getAsJsonObject();
            long price = Math.max(0, Json.number(i, "priceEach", 0)), qty = Math.max(0, Json.number(i, "quantity", 0));
            if (qty > 0 && price > (Long.MAX_VALUE - value) / qty) { value = Long.MAX_VALUE; }
            else { value += price * qty; }
        }
        if (wallMinGp >= 0 && value >= wallMinGp) { return true; }
        for (JsonElement valueEvent : events)
        {
            if (!valueEvent.isJsonObject()) { continue; }
            JsonObject event = valueEvent.getAsJsonObject();
            if (!SessionState.inWindow(event, at)) { continue; }
            JsonObject match = Json.object(event, "match");
            if ("all".equals(Json.text(match, "mode")) || contains(Json.array(match, "sources"), source)) { return true; }
            for (JsonElement item : items)
            {
                JsonObject i = item.getAsJsonObject();
                if (contains(Json.array(match, "itemIds"), Json.text(i, "id")) || contains(Json.array(match, "names"), Json.text(i, "name"))) { return true; }
            }
        }
        return false;
    }
    public static String normalize(String value)
    { return Json.norm(value).replaceAll("['‘’ʼ`“”\"]", ""); }
    private static boolean contains(JsonArray values, String needle)
    { for (JsonElement value : values) { if (value.isJsonPrimitive() && normalize(value.getAsString()).equals(normalize(needle))) { return true; } } return false; }
}
