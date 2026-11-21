package com.ironcladrs.events;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.loot.*;
import com.ironcladrs.events.sync.SessionState;
import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.*;

public class LootPolicyTest
{
    private IroncladEventsConfig config(boolean clan)
    {
        return new IroncladEventsConfig() {
            @Override public boolean useClanSettings() { return clan; }
            @Override public int minimumWallValue() { return 2000000; }
            @Override public String ignoredLootItems() { return " Coins, bones "; }
            @Override public String ignoredLootSources() { return "Abyssal demon"; }
        };
    }
    private SessionState state(int minimum)
    {
        return new SessionState(true, "", Instant.now(), new JsonObject(), Json.obj("clanPreset", Json.obj("version", 1,
            "filters", Json.obj("minimumWallValue", minimum, "ignoredLootItems", "Feather", "ignoredLootSources", "Zulrah"),
            "recommended", Json.obj("autoDrops", true, "deathScreenshots", true))));
    }
    private JsonArray items(String... names)
    { JsonArray result = new JsonArray(); for (String name : names) { result.add(Json.obj("name", name, "quantity", 1, "priceEach", 100)); } return result; }
    @Test public void exactNameExclusionsPreserveMixedDropPayloads()
    {
        LootPolicy policy = new LootPolicy(config(false), state(0));
        assertTrue(policy.ignores(" abyssal_demon ", items("Whip")));
        assertTrue(policy.ignores("Boss", items("COINS", "Bones")));
        JsonArray mixed = items("Coins", "Abyssal whip"); JsonArray before = mixed.deepCopy();
        assertFalse(policy.ignores("Boss", mixed)); assertEquals(before, mixed);
        assertFalse(policy.ignores("Greater abyssal demon", items("Coin")));
    }
    @Test public void higherWallMinimumDoesNotSuppressAnEventMatch()
    {
        LootPolicy policy = new LootPolicy(config(false), state(0)); assertEquals(2000000, policy.wallMinimum(1000000)); assertEquals(3000000, policy.wallMinimum(3000000));
        Instant now = Instant.now(); JsonArray events = new JsonArray(); events.add(Json.obj("state", "live", "startsAt", now.minusSeconds(30), "endsAt", now.plusSeconds(30), "match", Json.obj("mode", "all")));
        assertTrue(LootMatcher.matches(events, policy.wallMinimum(1000000), "Boss", items("Whip"), now));
        assertFalse(LootMatcher.matches(new JsonArray(), policy.wallMinimum(1000000), "Boss", items("Whip"), now));
    }
    @Test public void followingAndLeavingClanDefaultsPreservesPersonalFiltersAndConsent()
    {
        IroncladEventsConfig config = config(true); LootPolicy clan = new LootPolicy(config, state(5000000)); assertTrue(clan.clan); assertEquals(5000000, clan.minimumWallValue);
        assertTrue(clan.ignores("Zulrah", items("Whip"))); assertFalse(clan.ignores("Abyssal demon", items("Whip")));
        assertEquals(6000000, new LootPolicy(config, state(6000000)).minimumWallValue);
        LootPolicy personal = new LootPolicy(config(false), state(6000000)); assertFalse(personal.clan); assertEquals(2000000, personal.minimumWallValue);
        assertFalse(config.autoDrops()); assertFalse(config.deathScreenshots()); assertFalse(config.connect());
    }
    @Test public void missingPresetFallsBackToPersonalSettingsWithoutNetworking()
    { LootPolicy policy = new LootPolicy(config(true), SessionState.empty("")); assertFalse(policy.clan); assertEquals(2000000, policy.minimumWallValue); }
}
