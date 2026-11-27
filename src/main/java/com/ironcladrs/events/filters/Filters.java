package com.ironcladrs.events.filters;

import com.google.gson.JsonObject;
import com.ironcladrs.events.ClueTier;
import com.ironcladrs.events.IroncladEventsConfig;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.loot.LootMatcher;
import com.ironcladrs.events.sync.SessionState;
import java.util.*;

/**
 * Dink-style rules for what is sent (user, 2026-11-27: "more specific like dinks settings", and
 * clan staff setting them on the website). One snapshot per decision: the member's own settings,
 * or, with Use clan filters on, the clan's from the website (any rule the clan left out keeps the
 * member's own). Each feature asks this; nothing here sends anything.
 */
public final class Filters
{
    public final boolean clan;
    public final long screenshotMinValue;
    public final Set<String> alwaysSend, onlyAccounts, ignoredDeathRegions;
    public final boolean sendClueLoot, relayBroadcasts, petDuplicates, virtualLevels;
    public final int levelMinimum, levelInterval, totalLevelInterval, screenshotScale;
    public final ClueTier clueMinTier;

    public Filters(IroncladEventsConfig config, SessionState state)
    {
        JsonObject preset = Json.object(state.sync(), "clanPreset");
        if (preset.size() == 0) { preset = Json.object(state.hello(), "clanPreset"); }
        clan = config.useClanSettings() && Json.number(preset, "version", 0) == 1;
        JsonObject f = clan ? Json.object(preset, "filters") : new JsonObject();
        alwaysSend = names(text(f, "alwaysSendItems", config.alwaysSendItems()));
        screenshotMinValue = number(f, "screenshotMinValue", config.screenshotMinValue(), 0, Integer.MAX_VALUE);
        sendClueLoot = flag(f, "sendClueLoot", config.sendClueLoot());
        screenshotScale = (int) number(f, "screenshotScale", config.screenshotScale(), 25, 100);
        relayBroadcasts = flag(f, "relayBroadcasts", config.relayBroadcasts());
        ignoredDeathRegions = names(text(f, "ignoredDeathRegions", config.ignoredDeathRegions()));
        petDuplicates = flag(f, "petDuplicates", config.petDuplicates());
        levelMinimum = (int) number(f, "levelMinimum", config.levelMinimum(), 1, 126);
        levelInterval = (int) number(f, "levelInterval", config.levelInterval(), 1, 50);
        virtualLevels = flag(f, "virtualLevels", config.virtualLevels());
        totalLevelInterval = (int) number(f, "totalLevelInterval", config.totalLevelInterval(), 25, 500);
        clueMinTier = f.has("clueMinTier") ? ClueTier.of(Json.text(f, "clueMinTier")) : config.clueMinTier();
        // The account list is always the member's own: it is about their alts, not the clan.
        onlyAccounts = names(config.onlyAccounts());
    }

    // ── Rules ────────────────────────────────────────────────────────────────
    /** An empty list means every linked account. */
    public boolean accountAllowed(String rsn) { return onlyAccounts.isEmpty() || onlyAccounts.contains(LootMatcher.normalize(rsn)); }
    public boolean alwaysSends(String itemName) { return alwaysSend.contains(LootMatcher.normalize(itemName)); }
    public static boolean isClueLoot(String source) { return source != null && source.toLowerCase(Locale.ROOT).startsWith("clue scroll"); }
    /** A level worth announcing: at or above the minimum, on the interval. */
    public boolean announcesLevel(int level) { return level >= levelMinimum && (level - levelMinimum) % levelInterval == 0; }
    /** Crossing a multiple of the interval, from 500 total up. */
    public boolean announcesTotal(int before, int now) { return now >= 500 && now / totalLevelInterval > before / totalLevelInterval; }
    public int totalMilestone(int now) { return now / totalLevelInterval * totalLevelInterval; }
    public boolean sharesClue(String tier) { return ClueTier.of(tier).ordinal() >= clueMinTier.ordinal(); }
    public boolean ignoresDeathIn(int regionId) { return ignoredDeathRegions.contains(Integer.toString(regionId)); }

    // ── Reading the clan's values, falling back to the member's own ─────────
    private static String text(JsonObject f, String key, String own) { return f.has(key) ? Json.text(f, key) : own == null ? "" : own; }
    private static boolean flag(JsonObject f, String key, boolean own) { return f.has(key) ? Json.flag(f, key) : own; }
    private static long number(JsonObject f, String key, long own, long min, long max)
    { long v = f.has(key) ? Json.number(f, key, own) : own; return Math.max(min, Math.min(max, v)); }
    private static Set<String> names(String text)
    {
        Set<String> result = new HashSet<>();
        String bounded = text == null ? "" : text.substring(0, Math.min(1000, text.length()));
        for (String value : bounded.split("[,\\r\\n]")) { if (!value.trim().isEmpty()) { result.add(LootMatcher.normalize(value)); } }
        return result;
    }
}
