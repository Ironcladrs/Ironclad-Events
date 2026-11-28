package com.ironcladrs.events.loot;

import com.google.gson.*;
import com.ironcladrs.events.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.capture.Screenshots;
import com.ironcladrs.events.notice.ChatNotices;
import com.ironcladrs.events.queue.*;
import com.ironcladrs.events.sync.SyncService;
import java.time.Instant;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

/** A screenshot failure never loses a qualifying drop; Dink can still provide later proof. */
@Singleton
public class LootSubmitter
{
    @Inject private IroncladEventsConfig config;
    @Inject private PlayerGuard guard;
    @Inject private Consent consent;
    @Inject private SyncService sync;
    @Inject private Outbox outbox;
    @Inject private Screenshots screenshots;
    @Inject private ChatNotices notices;
    @Inject private com.ironcladrs.events.filters.RarityTable rarity;
    public void submit(JsonObject source, JsonArray items, Instant at)
    {
        LootPolicy policy = new LootPolicy(config, sync.state());
        if (!consent.allows("loot") || policy.ignores(Json.text(source, "name"), items)) { return; }
        com.ironcladrs.events.filters.Filters filters = new com.ironcladrs.events.filters.Filters(config, sync.state());
        if (!filters.sendClueLoot && com.ironcladrs.events.filters.Filters.isClueLoot(Json.text(source, "name"))) { return; }
        boolean pk = "PLAYER".equals(Json.text(source, "type"));
        if (pk && !filters.sendPkLoot) { return; }
        long total = 0; boolean always = false; int rarest = 0;
        for (JsonElement value : items)
        {
            if (!value.isJsonObject()) { continue; } JsonObject item = value.getAsJsonObject();
            total += Json.number(item, "quantity", 0) * Json.number(item, "priceEach", 0); always |= filters.alwaysSends(Json.text(item, "name"));
            rarest = Math.max(rarest, rarity.oneIn((int) Json.number(item, "id", 0)));
        }
        boolean rare = !pk && filters.rare(rarest);
        // Event tiles and the Wall of Fame are separate switches. A drop that matches an event only goes
        // while Event tile drops is on, because sending it would file a claim.
        String name = Json.text(source, "name");
        boolean event = LootMatcher.matches(sync.state().events(), -1, name, items, at);
        boolean valued = LootMatcher.matches(new JsonArray(), policy.wallMinimum(Json.number(Json.object(sync.state().hello(), "wall"), "minGp", Long.MAX_VALUE)), name, items, at);
        // Rare drops skip the value minimum unless "Rare drops must also meet value" is on.
        boolean viaRarity = rare && (!filters.rarityAndValue || valued);
        boolean wall = always || viaRarity || valued;
        if (pk) { event = false; }
        if (event ? !config.autoDrops() : !(wall && config.wallOfFameDrops())) { return; }
        long generation = guard.generation();
        JsonObject payload = Json.obj("occurredAt", at, "source", source, "items", items, "wallOfFame", config.wallOfFameDrops());
        // Tells the site why a drop below the minimum was sent, so it still reaches the wall.
        if (!valued && (always || viaRarity)) { payload.addProperty("wallReason", always ? "always" : "rare"); }
        Consumer<byte[]> send = image -> {
            if (generation != guard.generation() || !consent.allows("loot")) { return; }
            outbox.submit("loot", "/loot", payload, image, result -> {
                if (!result.success() || Json.flag(result.data, "duplicate")) { return; }
                String item = Json.text(items.get(0).getAsJsonObject(), "name");
                for (JsonElement filed : Json.array(result.data, "filed")) { if (filed.isJsonObject()) { notices.filed(item, Json.text(filed.getAsJsonObject(), "label"), Json.text(filed.getAsJsonObject(), "kind") + ":" + Json.text(filed.getAsJsonObject(), "id")); } }
            });
        };
        // Event drops always carry proof; other drops only from the screenshot minimum up.
        if (config.screenshots() && (event || rare || total >= filters.screenshotMinValue)) { screenshots.capture(send); } else { send.accept(null); }
    }
}
