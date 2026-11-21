package com.ironcladrs.events;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.config.RuneScapeProfileType;

/** Capture on the client thread. No off-thread code reads game state or account identity. */
@Singleton
public class PlayerGuard
{
    @Inject private Client client;
    @Inject private IroncladEventsConfig config;
    private volatile Identity identity;
    private volatile long generation;
    private boolean active;

    public synchronized boolean refresh()
    {
        Identity next = null;
        if (active && config.connect() && config.apiKey().matches("ice_[A-Za-z0-9_-]{43}")
            && client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null
            && RuneScapeProfileType.getCurrent(client) == RuneScapeProfileType.STANDARD)
        {
            JsonArray worlds = new JsonArray();
            client.getWorldType().forEach(type -> worlds.add(type.name()));
            next = new Identity(client.getLocalPlayer().getName(), client.getAccountHash(), config.apiKey(), worlds);
        }
        if (!Objects.equals(identity, next)) { identity = next; generation++; return true; }
        return false;
    }
    public Identity identity() { return identity; }
    public long generation() { return generation; }
    public synchronized void activate() { active = true; identity = null; generation++; }
    public synchronized void deactivate() { active = false; identity = null; generation++; }
    public synchronized void clear() { identity = null; generation++; }

    public static final class Identity
    {
        public final String rsn;
        public final long accountHash;
        private final String key;
        private final JsonArray worlds;
        Identity(String rsn, long accountHash, String key, JsonArray worlds)
        { this.rsn = rsn == null ? "" : rsn.replace('\u00a0', ' '); this.accountHash = accountHash; this.key = key; this.worlds = worlds.deepCopy(); }
        public String key() { return key; }
        public JsonObject payload()
        { return Json.obj("rsn", rsn, "accountHash", Long.toString(accountHash), "worldTypes", worlds.deepCopy()); }
        @Override public boolean equals(Object other)
        {
            if (!(other instanceof Identity)) { return false; }
            Identity i = (Identity) other;
            return accountHash == i.accountHash && Objects.equals(rsn, i.rsn) && Objects.equals(key, i.key)
                && worlds.equals(i.worlds);
        }
        @Override public int hashCode() { return Objects.hash(rsn, accountHash, key, worlds); }
        // No toString(): a bearer key must never enter logs.
    }
}
