package com.ironcladrs.events;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.ui.TaskList;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Tasks arrive interleaved across categories; the panel shows each category once, in first-seen order. */
public class TaskListTest
{
    @Test public void tasksAreGroupedUnderOneHeadingPerCategory()
    {
        JsonArray tasks = new JsonArray();
        String[][] rows = {{"Killcount", "Kraken"}, {"PvM drops", "Trident"}, {"Killcount", "Zalcano"}, {"Hiscore goals", "Runecrafting"}, {"PvM drops", "Thrownaxe"}};
        for (String[] row : rows) { tasks.add(Json.obj("category", row[0], "label", row[1])); }
        List<String> order = new ArrayList<>();
        for (JsonObject task : TaskList.byCategory(tasks)) { order.add(Json.text(task, "category") + ":" + Json.text(task, "label")); }
        assertEquals(Arrays.asList("Killcount:Kraken", "Killcount:Zalcano", "PvM drops:Trident", "PvM drops:Thrownaxe", "Hiscore goals:Runecrafting"), order);
    }

    /** Visible through every parent up to the list? */
    private static boolean shown(java.awt.Component c, java.awt.Component root) { for (; c != null && c != root; c = c.getParent()) { if (!c.isVisible()) { return false; } } return true; }
    private static List<String> visibleTitles(java.awt.Container root, java.awt.Container at, Set<String> titles)
    {
        List<String> out = new ArrayList<>();
        for (java.awt.Component c : at.getComponents())
        {
            if (c instanceof javax.swing.JLabel) { String t = ((javax.swing.JLabel) c).getText(); for (String title : titles) { if (t != null && t.contains(title) && shown(c, root)) { out.add(title); } } }
            if (c instanceof java.awt.Container) { out.addAll(visibleTitles(root, (java.awt.Container) c, titles)); }
        }
        return out;
    }
    private static javax.swing.JTextField box(java.awt.Container at)
    {
        for (java.awt.Component c : at.getComponents())
        { if (c instanceof javax.swing.JTextField) { return (javax.swing.JTextField) c; } if (c instanceof java.awt.Container) { javax.swing.JTextField f = box((java.awt.Container) c); if (f != null) { return f; } } }
        return null;
    }

    /** User, 2026-11-27: "search tasks by bosses, drop, activity". The server's search words count too. */
    @Test public void searchFindsTasksByBossItemOrActivity()
    {
        JsonArray tasks = new JsonArray();
        tasks.add(Json.obj("id", "a", "category", "Boss drops", "label", "Fangs", "search", "tanzanite fang · magic fang · Zulrah · Boss drops"));
        tasks.add(Json.obj("id", "b", "category", "Boss KC", "label", "Vorkath KC", "search", "Vorkath · Boss KC"));
        tasks.add(Json.obj("id", "c", "category", "Skilling", "label", "Agility laps", "search", "Rooftop courses · Skill training"));
        TaskList list = new TaskList(tasks, null);
        Set<String> titles = new HashSet<>(Arrays.asList("Fangs", "Vorkath KC", "Agility laps"));
        javax.swing.JTextField search = box(list);
        search.setText("zulrah"); assertEquals(Arrays.asList("Fangs"), visibleTitles(list, list, titles));
        search.setText("magic fang"); assertEquals(Arrays.asList("Fangs"), visibleTitles(list, list, titles));
        search.setText("rooftop"); assertEquals(Arrays.asList("Agility laps"), visibleTitles(list, list, titles));
        search.setText(""); assertEquals(3, visibleTitles(list, list, titles).size());
    }
}
