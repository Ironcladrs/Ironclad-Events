package com.ironcladrs.events.loot;

import com.google.gson.*;
import com.ironcladrs.events.IroncladEventsConfig;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.sync.SessionState;
import java.util.*;

/** Filters never alter the item list used by Dink de-duplication or enable uploads. */
public final class LootPolicy
{
    public final int minimumWallValue;
    public final String ignoredItems, ignoredSources;
    public final boolean clan;
    public LootPolicy(IroncladEventsConfig config, SessionState state)
    {
        JsonObject preset = Json.object(state.sync(), "clanPreset");
        if (preset.size() == 0) { preset = Json.object(state.hello(), "clanPreset"); }
        clan = config.useClanSettings() && Json.number(preset, "version", 0) == 1;
        JsonObject filters = Json.object(preset, "filters");
        minimumWallValue = clan ? Json.bounded(filters, "minimumWallValue", 0, 0, Integer.MAX_VALUE) : Math.max(0, config.minimumWallValue());
        ignoredItems = bounded(clan ? Json.text(filters, "ignoredLootItems") : config.ignoredLootItems());
        ignoredSources = bounded(clan ? Json.text(filters, "ignoredLootSources") : config.ignoredLootSources());
    }
    private static String bounded(String value) { return value == null ? "" : value.substring(0, Math.min(500, value.length())); }
    public long wallMinimum(long serverMinimum) { return Math.max(serverMinimum, minimumWallValue); }
    public boolean ignores(String source, JsonArray items)
    {
        if (names(ignoredSources).contains(LootMatcher.normalize(source))) { return true; }
        Set<String> ignored = names(ignoredItems);
        if (items.size() == 0) { return false; }
        for (JsonElement value : items)
        { if (!value.isJsonObject() || !ignored.contains(LootMatcher.normalize(Json.text(value.getAsJsonObject(), "name")))) { return false; } }
        return true;
    }
    private static Set<String> names(String text)
    {
        Set<String> result = new HashSet<>();
        for (String value : text.split("[,\\r\\n]")) { if (!value.trim().isEmpty()) { result.add(LootMatcher.normalize(value)); } }
        return result;
    }
}
