package com.ironcladrs.events;

import com.google.gson.JsonObject;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.death.SafeDeaths;
import com.ironcladrs.events.filters.Filters;
import com.ironcladrs.events.filters.RarityTable;
import com.ironcladrs.events.sync.SessionState;
import java.time.Instant;
import java.util.EnumSet;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Rarity override, safe deaths, combat task and diary tiers (user, 2026-11-27: "make it better than dink"). */
public class BetterThanDinkTest
{
    private static Client client(boolean hardcoreWorld, int ironman)
    {
        Client client = mock(Client.class);
        when(client.getWorldType()).thenReturn(hardcoreWorld ? EnumSet.of(WorldType.MEMBERS, WorldType.HARDCORE_MAIN) : EnumSet.of(WorldType.MEMBERS));
        when(client.getVarbitValue(VarbitID.IRONMAN)).thenReturn(ironman);
        return client;
    }

    @Test public void safePlacesAreSafeHardcoreAlwaysCountsAndChosenExceptionsStillCount()
    {
        EnumSet<SafeDeathException> none = EnumSet.noneOf(SafeDeathException.class);
        assertTrue("Castle Wars", SafeDeaths.safe(client(false, 0), 9520, none));
        assertTrue("ToB for a main", SafeDeaths.safe(client(false, 0), 12611, none));
        assertFalse("ToB for a hardcore ironman", SafeDeaths.safe(client(false, 3), 12611, none));
        assertFalse("anything raid on a hardcore world", SafeDeaths.safe(client(true, 0), 13136, none));
        assertFalse("raids chosen to still count", SafeDeaths.safe(client(false, 0), 12611, EnumSet.of(SafeDeathException.RAIDS)));
        assertTrue("Inferno is safe by itself", SafeDeaths.safe(client(false, 0), 9043, none));
        assertFalse("Inferno chosen to still count", SafeDeaths.safe(client(false, 0), 9043, EnumSet.of(SafeDeathException.INFERNO)));
        assertFalse("ordinary places are dangerous", SafeDeaths.safe(client(false, 0), 12850, none));
        Client zulrah = client(false, 0);
        when(zulrah.getVarbitValue(VarbitID.ZULRAH_REVIVE)).thenReturn(0); when(zulrah.getVarbitValue(VarbitID.WESTERN_DIARY_ELITE_COMPLETE)).thenReturn(1);
        assertTrue("the daily diary revive at Zulrah", SafeDeaths.safe(zulrah, 9007, none));
        assertFalse(SafeDeaths.safe(zulrah, 9007, EnumSet.of(SafeDeathException.DIARY_REVIVE)));
    }

    @Test public void rarityTableAndOverride()
    {
        RarityTable table = new RarityTable();
        table.update(Json.obj("11286", 5000, "526", 1, "bad", 3, "4151", "512"));
        assertEquals(5000, table.oneIn(11286)); assertEquals(0, table.oneIn(526)); assertEquals(512, table.oneIn(4151)); assertEquals(0, table.oneIn(999));
        Filters f = new Filters(new IroncladEventsConfig() {}, SessionState.empty(""));
        assertEquals("starts at 1 in 1,000", 1000, f.rarityOverride);
        assertTrue(f.rare(5000)); assertFalse(f.rare(512)); assertFalse("unknown odds are never rare", f.rare(0));
        Filters off = new Filters(new IroncladEventsConfig() { @Override public int rarityOverride() { return 0; } }, SessionState.empty(""));
        assertFalse(off.rare(1_000_000));
    }

    @Test public void combatTaskAndDiaryTiersAndClanValues()
    {
        Filters own = new Filters(new IroncladEventsConfig()
        { @Override public CombatTaskTier combatTaskMinTier() { return CombatTaskTier.ELITE; } @Override public DiaryTier diaryMinTier() { return DiaryTier.HARD; } }, SessionState.empty(""));
        assertTrue(own.announcesCombatTask("grandmaster")); assertFalse(own.announcesCombatTask("hard"));
        assertTrue(own.announcesDiary("elite")); assertFalse(own.announcesDiary("medium"));
        assertEquals(EnumSet.of(SafeDeathException.INFERNO, SafeDeathException.FIGHT_CAVE, SafeDeathException.JAD_CHALLENGES), own.safeDeathExceptions);
        JsonObject preset = Json.obj("version", 1, "filters", Json.obj("caMinTier", "master", "safeDeathExceptions", "raids, gauntlet", "sendPkLoot", false));
        Filters clan = new Filters(new IroncladEventsConfig() { @Override public boolean useClanSettings() { return true; } },
            new SessionState(true, "", Instant.now(), new JsonObject(), Json.obj("clanPreset", preset)));
        assertEquals(CombatTaskTier.MASTER, clan.combatTaskMinTier);
        assertEquals(EnumSet.of(SafeDeathException.RAIDS, SafeDeathException.GAUNTLET), clan.safeDeathExceptions);
        assertFalse(clan.sendPkLoot);
    }
}
