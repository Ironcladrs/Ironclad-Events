package com.ironcladrs.events.filters;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;

/**
 * "1 in N" for rare items, sent by ironcladrs.com once per session (from OSRSClans' drop chances, the
 * item's most common source). Replaced whole on each hello; read by the rarity override. Unknown: 0.
 */
@Singleton
public class RarityTable
{
    private volatile Map<Integer, Integer> oneIn = Collections.emptyMap();
    public void update(JsonObject table)
    {
        if (table == null || table.size() == 0) { return; }
        Map<Integer, Integer> next = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : table.entrySet())
        {
            if (next.size() >= 50_000) { break; }
            try { int id = Integer.parseInt(e.getKey()), n = e.getValue().getAsInt(); if (id > 0 && n > 1) { next.put(id, n); } }
            catch (RuntimeException ignored) { /* A malformed entry is skipped. */ }
        }
        oneIn = Collections.unmodifiableMap(next);
    }
    public int oneIn(int itemId) { return oneIn.getOrDefault(itemId, 0); }
}
