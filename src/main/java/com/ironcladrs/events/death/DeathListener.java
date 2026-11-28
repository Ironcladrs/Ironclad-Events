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
    @Inject private com.ironcladrs.events.sync.SyncService sync;
    private int lastTick = -1;
    @Subscribe public void onActorDeath(ActorDeath event)
    {
        if (client.getLocalPlayer() == null || event.getActor() != client.getLocalPlayer() || !consent.allows("deaths")) { return; }
        int tick = client.getTickCount(); if (tick == lastTick) { return; } lastTick = tick;
        // Deaths in regions the member (or the clan) listed, such as safe minigames, are never announced.
        int region = -1;
        try
        {
            net.runelite.api.coords.LocalPoint here = client.getLocalPlayer().getLocalLocation();
            if (here != null) { region = net.runelite.api.coords.WorldPoint.fromLocalInstance(client, here).getRegionID(); }
        }
        catch (RuntimeException ignored) { region = -1; /* Location unknown: announce as usual. */ }
        com.ironcladrs.events.filters.Filters filters = new com.ironcladrs.events.filters.Filters(config, sync.state());
        if (region >= 0 && filters.ignoresDeathIn(region)) { return; }
        // Nothing lost: not Wall of Shame material (Dink's rules, SafeDeaths), unless chosen to still count.
        if (region >= 0 && filters.ignoreSafeDeaths && SafeDeaths.safe(client, region, filters.safeDeathExceptions)) { return; }
        com.google.gson.JsonObject payload = Json.obj("at", Instant.now());
        if (config.deathScreenshots()) { screenshots.capture(bytes -> outbox.submit("deaths", "/death", payload, bytes, ignored -> {})); }
        else { outbox.submit("deaths", "/death", payload, null, ignored -> {}); }
    }
    public void reset() { lastTick = -1; }
}
