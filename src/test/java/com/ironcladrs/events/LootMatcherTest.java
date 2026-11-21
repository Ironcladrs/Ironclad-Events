package com.ironcladrs.events;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.loot.LootMatcher;
import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.*;

public class LootMatcherTest
{
    private final Instant now = Instant.parse("2026-11-17T12:00:00Z");
    private JsonArray items() { JsonArray a = new JsonArray(); a.add(Json.obj("id", 4151, "name", "Abyssal whip", "quantity", 2, "priceEach", 500000)); return a; }
    private JsonArray events(JsonObject match)
    { JsonArray a = new JsonArray(); a.add(Json.obj("state", "live", "startsAt", now.minusSeconds(60), "endsAt", now.plusSeconds(60), "match", match)); return a; }
    @Test public void listIdNameAndSourceMatch()
    {
        JsonArray ids = new JsonArray(); ids.add(4151);
        assertTrue(LootMatcher.matches(events(Json.obj("mode", "list", "itemIds", ids)), Long.MAX_VALUE, "Abyssal demon", items(), now));
        JsonArray names = new JsonArray(); names.add(" ABYSSAL WHIP ");
        assertTrue(LootMatcher.matches(events(Json.obj("names", names)), Long.MAX_VALUE, "boss", items(), now));
        JsonArray sources = new JsonArray(); sources.add(" abyssal demon ");
        assertTrue(LootMatcher.matches(events(Json.obj("sources", sources)), Long.MAX_VALUE, "Abyssal demon", items(), now));
    }
    @Test public void hiddenAllStillRequiresLiveWindow()
    {
        JsonArray events = events(Json.obj("mode", "all"));
        assertTrue(LootMatcher.matches(events, Long.MAX_VALUE, "boss", items(), now));
        assertFalse(LootMatcher.matches(events, Long.MAX_VALUE, "boss", items(), now.plusSeconds(60)));
        events.get(0).getAsJsonObject().addProperty("state", "upcoming");
        assertFalse(LootMatcher.matches(events, Long.MAX_VALUE, "boss", items(), now));
    }
    @Test public void exactWallThresholdAndUnmatchedDrop()
    {
        assertTrue(LootMatcher.matches(new JsonArray(), 1000000, "boss", items(), now));
        assertFalse(LootMatcher.matches(new JsonArray(), 1000001, "boss", items(), now));
        assertFalse(LootMatcher.matches(events(new JsonObject()), Long.MAX_VALUE, "boss", items(), now));
    }
}
