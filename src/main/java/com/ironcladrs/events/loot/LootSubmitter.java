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
    public void submit(JsonObject source, JsonArray items, Instant at)
    {
        LootPolicy policy = new LootPolicy(config, sync.state());
        if (!consent.allows("loot") || policy.ignores(Json.text(source, "name"), items)) { return; }
        // Event tiles and the Wall of Fame are separate switches. A drop that matches an event only goes
        // while Event tile drops is on, because sending it would file a claim.
        String name = Json.text(source, "name");
        boolean event = LootMatcher.matches(sync.state().events(), -1, name, items, at);
        boolean wall = LootMatcher.matches(new JsonArray(), policy.wallMinimum(Json.number(Json.object(sync.state().hello(), "wall"), "minGp", Long.MAX_VALUE)), name, items, at);
        if (event ? !config.autoDrops() : !(wall && config.wallOfFameDrops())) { return; }
        long generation = guard.generation();
        JsonObject payload = Json.obj("occurredAt", at, "source", source, "items", items, "wallOfFame", config.wallOfFameDrops());
        Consumer<byte[]> send = image -> {
            if (generation != guard.generation() || !consent.allows("loot")) { return; }
            outbox.submit("loot", "/loot", payload, image, result -> {
                if (!result.success() || Json.flag(result.data, "duplicate")) { return; }
                String item = Json.text(items.get(0).getAsJsonObject(), "name");
                for (JsonElement filed : Json.array(result.data, "filed")) { if (filed.isJsonObject()) { notices.filed(item, Json.text(filed.getAsJsonObject(), "label"), Json.text(filed.getAsJsonObject(), "kind") + ":" + Json.text(filed.getAsJsonObject(), "id")); } }
            });
        };
        if (config.screenshots()) { screenshots.capture(send); } else { send.accept(null); }
    }
}
