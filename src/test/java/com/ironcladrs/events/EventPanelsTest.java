package com.ironcladrs.events;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.ui.EventCard;
import com.ironcladrs.events.ui.IroncladPanel;
import com.ironcladrs.events.ui.TaskList;
import java.awt.Component;
import java.awt.Container;
import java.util.*;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Domination's close calls and regions, Battleship Bingo's enemy fleet, and group headings (2026-11-27). */
public class EventPanelsTest
{
    private static void texts(Container at, List<String> out)
    {
        for (Component c : at.getComponents())
        {
            if (c instanceof JLabel && ((JLabel) c).getText() != null) { out.add(((JLabel) c).getText().replaceAll("<[^>]*>", "")); }
            if (c instanceof AbstractButton && ((AbstractButton) c).getText() != null) { out.add(((AbstractButton) c).getText()); }
            if (c instanceof Container) { texts((Container) c, out); }
        }
    }
    private static String shown(JsonObject event)
    {
        EventCard card = new EventCard(event, new JsonArray(), null, mock(IroncladPanel.Actions.class), null, false, new HashSet<>());
        List<String> out = new ArrayList<>(); texts(card, out); return String.join(" | ", out);
    }
    private static JsonObject event(String type)
    {
        return Json.obj("id", 9, "name", "Test", "type", type, "state", "live", "view", Json.obj("version", 1, "mode", "standard", "message", ""));
    }

    @Test public void dominationShowsCloseCallsAndRegions()
    {
        JsonObject ev = event("dom");
        JsonArray close = new JsonArray(); close.add(Json.obj("name", "Vorkath", "kind", "reach", "text", "2 behind Red Dragons")); close.add(Json.obj("name", "Zulrah", "kind", "risk", "text", "Your lead over Blue Moons is 1"));
        JsonArray areas = new JsonArray(); areas.add(Json.obj("name", "Kandarin", "status", "theirs", "text", "Held by Red Dragons")); areas.add(Json.obj("name", "Asgarnia", "status", "yours", "text", "Held by your team"));
        ev.add("closeCalls", close); ev.add("areas", areas);
        String text = shown(ev);
        for (String expected : new String[]{"Close calls", "Within reach", "Vorkath", "2 behind Red Dragons", "At risk", "Regions", "1 of 2 yours", "Theirs", "Kandarin", "Yours"})
        { assertTrue(expected + " in: " + text, text.contains(expected)); }
        assertFalse(text.contains("Enemy fleet"));
    }

    @Test public void battleshipShowsTheEnemyFleetAsItSinks()
    {
        JsonObject ev = event("battleship");
        JsonArray ships = new JsonArray(); ships.add(Json.obj("label", "Carrier", "length", 5, "sunk", false)); ships.add(Json.obj("label", "Destroyer", "length", 2, "sunk", true));
        JsonArray buoys = new JsonArray(); buoys.add(Json.obj("label", "Recon", "found", 1, "total", 2));
        JsonObject fleet = Json.obj("enemyName", "Pirates", "shots", Json.obj("hits", 3, "misses", 4), "yourFleet", Json.obj("afloat", 2, "total", 2));
        fleet.add("ships", ships); fleet.add("buoys", buoys); ev.add("fleet", fleet);
        String text = shown(ev);
        for (String expected : new String[]{"Enemy fleet: Pirates", "1 of 2 sunk", "Afloat", "Carrier (5)", "Sunk", "Destroyer (2)", "Buoys found", "Recon", "1 of 2", "3 hit, 4 miss", "2 of 2 afloat"})
        { assertTrue(expected + " in: " + text, text.contains(expected)); }
        assertFalse(text.contains("Regions"));
    }

    @Test public void tasksUseTheSitesGroupForHeadings()
    {
        assertEquals("Kandarin", TaskList.heading(Json.obj("group", "Kandarin", "category", "Bosses")));
        assertEquals("Bosses", TaskList.heading(Json.obj("category", "Bosses")));
    }
}
