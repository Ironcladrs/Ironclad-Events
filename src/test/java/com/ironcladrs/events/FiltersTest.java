package com.ironcladrs.events;

import com.google.gson.JsonObject;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.filters.Filters;
import com.ironcladrs.events.sync.SessionState;
import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.*;

public class FiltersTest
{
    private static SessionState withPreset(JsonObject filters)
    { return new SessionState(true, "", Instant.now(), new JsonObject(), Json.obj("clanPreset", Json.obj("version", 1, "filters", filters))); }

    @Test public void ownSettingsApplyUntilClanFiltersAreOn()
    {
        IroncladEventsConfig own = new IroncladEventsConfig()
        {
            @Override public int levelMinimum() { return 70; }
            @Override public int levelInterval() { return 5; }
            @Override public String alwaysSendItems() { return "Dragon claws, Pet rock"; }
            @Override public String onlyAccounts() { return "Main Name"; }
        };
        SessionState clanSays = withPreset(Json.obj("levelMinimum", 90, "relayBroadcasts", false));
        Filters mine = new Filters(own, clanSays);
        assertFalse(mine.clan);
        assertTrue(mine.announcesLevel(70)); assertTrue(mine.announcesLevel(85)); assertFalse(mine.announcesLevel(86)); assertFalse(mine.announcesLevel(65));
        assertTrue(mine.alwaysSends("dragon claws")); assertFalse(mine.alwaysSends("Bones"));
        assertTrue(mine.accountAllowed("main name")); assertFalse(mine.accountAllowed("My Alt"));

        IroncladEventsConfig following = new IroncladEventsConfig()
        {
            @Override public boolean useClanSettings() { return true; }
            @Override public int levelInterval() { return 5; }
            @Override public String onlyAccounts() { return "Main Name"; }
        };
        Filters clan = new Filters(following, clanSays);
        assertTrue(clan.clan);
        assertEquals("the clan's minimum", 90, clan.levelMinimum);
        assertEquals("a rule the clan left out keeps the member's own", 5, clan.levelInterval);
        assertFalse(clan.relayBroadcasts);
        assertFalse("the account list is always the member's own", clan.accountAllowed("My Alt"));
    }

    @Test public void totalsCluesAndRegionsFollowTheirRules()
    {
        Filters f = new Filters(new IroncladEventsConfig()
        {
            @Override public int totalLevelInterval() { return 25; }
            @Override public ClueTier clueMinTier() { return ClueTier.HARD; }
            @Override public String ignoredDeathRegions() { return "9520, 12345"; }
        }, SessionState.empty(""));
        assertTrue(f.announcesTotal(1499, 1500)); assertTrue(f.announcesTotal(1524, 1525)); assertFalse(f.announcesTotal(1525, 1530));
        assertFalse("nothing below 500 total", f.announcesTotal(470, 475));
        assertEquals(1525, f.totalMilestone(1530));
        assertTrue(f.sharesClue("hard")); assertTrue(f.sharesClue("master")); assertFalse(f.sharesClue("medium"));
        assertTrue(f.ignoresDeathIn(9520)); assertFalse(f.ignoresDeathIn(9521));
        assertTrue(Filters.isClueLoot("Clue Scroll (Elite)")); assertFalse(Filters.isClueLoot("Vorkath"));
    }

    @Test public void outOfRangeClanValuesAreClamped()
    {
        Filters f = new Filters(new IroncladEventsConfig() { @Override public boolean useClanSettings() { return true; } },
            withPreset(Json.obj("levelInterval", 0, "screenshotScale", 5, "totalLevelInterval", 10_000)));
        assertEquals(1, f.levelInterval); assertEquals(25, f.screenshotScale); assertEquals(500, f.totalLevelInterval);
    }
}
