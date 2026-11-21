package com.ironcladrs.events;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.loot.LootMatcher;
import com.ironcladrs.events.sync.*;
import com.ironcladrs.events.ui.EventCard;
import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.*;

public class EventViewTest
{
    private JsonObject event(JsonObject view)
    {
        return Json.obj("id", 7, "name", "Future event", "type", "unknown-engine", "typeLabel", "Clan event", "state", "live",
            "startsAt", "2000-01-01T00:00:00Z", "endsAt", "2100-01-01T00:00:00Z", "url", "https://ironcladrs.com/events/7",
            "view", view, "match", Json.obj("mode", "all"), "tasks", new JsonArray());
    }
    @Test public void futureEventTypesUseTheStandardContractWithoutAClientRegistry()
    {
        JsonObject event = event(Json.obj("version", 1, "mode", "standard", "futureField", "ignored"));
        assertTrue(EventView.supported(event)); assertTrue(SessionState.inWindow(event, Instant.now()));
        event.remove("view"); assertTrue(EventView.supported(event));
    }
    @Test public void unknownContractsStayVisibleButCannotRequestAutomaticEventUploads()
    {
        for (JsonObject view : new JsonObject[]{Json.obj("version", 1.5, "mode", "standard"), Json.obj("version", "1", "mode", "standard"), Json.obj("version", 2, "mode", "standard"), Json.obj("version", 1, "mode", "new-layout"), Json.obj("version", 1, "mode", "generic"), new JsonObject()})
        {
            JsonObject event = event(view); JsonArray events = new JsonArray(); events.add(event);
            SessionState state = new SessionState(true, "", Instant.now(), new JsonObject(), Json.obj("events", events));
            assertEquals(1, state.events().size()); assertFalse(state.hasLiveEvent(Instant.now()));
            JsonObject safe = state.events().get(0).getAsJsonObject(); assertEquals("none", Json.text(Json.object(safe, "match"), "mode"));
            assertFalse(LootMatcher.matches(events, -1, "Boss", new JsonArray(), Instant.now()));
            assertEquals(0, Json.array(safe, "prechecksOwed").size());
        }
    }
    @Test public void fallbackRendersAWebsiteHandoffWithoutNeedingEngineComponents() throws Exception
    {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            EventCard card = new EventCard(event(Json.obj("version", 1, "mode", "generic")), new JsonArray(), null, null, null, false);
            boolean open = false;
            for (java.awt.Component component : card.getComponents())
            {
                if (component instanceof javax.swing.JButton) { assertEquals("Open event", ((javax.swing.JButton) component).getText()); open = true; }
                if (component instanceof javax.swing.JLabel) { assertFalse(((javax.swing.JLabel) component).getText().equals("Tasks")); }
            }
            assertTrue(open);
            assertTrue(EventView.message(event(Json.obj("version", 99, "mode", "generic"))).contains("paused"));
        });
    }
}
