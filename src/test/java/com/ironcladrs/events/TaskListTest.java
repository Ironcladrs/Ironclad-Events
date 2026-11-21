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
}
