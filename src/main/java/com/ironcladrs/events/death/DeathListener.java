package com.ironcladrs.events.death;

import com.ironcladrs.events.IroncladEventsConfig;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.capture.Screenshots;
import com.ironcladrs.events.queue.*;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.events.ActorDeath;
import net.runelite.client.eventbus.Subscribe;

/** Post-death notification of the local player only. No wealth, position or killer inspection. */
@Singleton
public class DeathListener
{
    @Inject private Client client;
    @Inject private IroncladEventsConfig config;
    @Inject private Consent consent;
    @Inject private Screenshots screenshots;
    @Inject private Outbox outbox;
    private int lastTick = -1;
    @Subscribe public void onActorDeath(ActorDeath event)
    {
        if (client.getLocalPlayer() == null || event.getActor() != client.getLocalPlayer() || !consent.allows("deaths")) { return; }
        int tick = client.getTickCount(); if (tick == lastTick) { return; } lastTick = tick;
        com.google.gson.JsonObject payload = Json.obj("at", Instant.now());
        if (config.deathScreenshots()) { screenshots.capture(bytes -> outbox.submit("deaths", "/death", payload, bytes, ignored -> {})); }
        else { outbox.submit("deaths", "/death", payload, null, ignored -> {}); }
    }
    public void reset() { lastTick = -1; }
}
