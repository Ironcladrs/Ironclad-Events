package com.ironcladrs.events.loot;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.progress.KillCountCollector;
import com.ironcladrs.events.queue.Consent;
import java.time.Instant;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.http.api.loottracker.LootRecordType;

/** Collect only the local loot events from §8.1; player loot has no subscription. */
@Singleton
public class LootCollector
{
    @Inject private Client client;
    @Inject private ItemManager items;
    @Inject private KillCountCollector kc;
    @Inject private LootSubmitter submitter;
    @Inject private Consent consent;
    @Subscribe public void onServerNpcLoot(ServerNpcLoot event)
    {
        if (!consent.allows("loot")) { return; }
        collect(event.getItems(), event.getComposition().getName(), "NPC", event.getComposition().getId());
    }
    /** Loot from a player kill: the other player's name never leaves the client (only "Player kill"). */
    @Subscribe public void onPlayerLootReceived(PlayerLootReceived event)
    {
        if (!consent.allows("loot")) { return; }
        collect(event.getItems(), "Player kill", "PLAYER", null);
    }
    @Subscribe public void onLootReceived(LootReceived event)
    {
        if (consent.allows("loot") && (event.getType() == LootRecordType.EVENT || event.getType() == LootRecordType.PICKPOCKET))
        { collect(event.getItems(), event.getName(), event.getType().name(), null); }
    }
    private void collect(Collection<ItemStack> stacks, String name, String type, Integer npcId)
    {
        if (name == null) { return; }
        KillCountCollector.Count count = kc.recent(name, client.getTickCount());
        String sourceName = count != null && count.name.startsWith(name + ":") ? count.name : name;
        JsonObject source = Json.obj("type", type, "name", sourceName, "npcId", npcId);
        if (count != null) { source.addProperty("killCount", count.count); }
        Map<Integer, Long> reduced = stack(stacks);
        JsonArray serialized = new JsonArray();
        reduced.forEach((id, quantity) -> serialized.add(Json.obj("id", id,
            "name", items.getItemComposition(id).getMembersName(), "quantity", quantity, "priceEach", items.getItemPrice(id))));
        if (serialized.size() > 0) { submitter.submit(source, serialized, Instant.now()); }
    }
    public static Map<Integer, Long> stack(Collection<ItemStack> stacks)
    {
        Map<Integer, Long> reduced = new LinkedHashMap<>();
        for (ItemStack item : stacks) { if (item.getId() >= 0 && item.getQuantity() > 0) { reduced.merge(item.getId(), (long) item.getQuantity(), Long::sum); } }
        return reduced;
    }
}
