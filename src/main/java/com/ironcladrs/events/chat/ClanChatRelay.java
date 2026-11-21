package com.ironcladrs.events.chat;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.queue.*;
import com.ironcladrs.events.sync.SyncService;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;

/** Typed clan chat only by default. Guest, friends, public and private messages never enter the buffer. */
@Singleton
public class ClanChatRelay
{
    @Inject private Client client;
    @Inject private Consent consent;
    @Inject private Outbox outbox;
    @Inject private SyncService sync;
    private JsonArray messages = new JsonArray();
    private Instant due = Instant.EPOCH;
    @Subscribe public void onChatMessage(ChatMessage event)
    {
        if (!consent.allows("chat")) { return; }
        ClanChannel channel = client.getClanChannel();
        if (channel == null || !accepts(event.getType(), channel.getName(), sync.state().clanName())) { return; }
        // System broadcasts are deliberately off until there is an explicit server opt-in policy.
        String author = MessageSanitizer.sanitize(event.getName()), content = MessageSanitizer.sanitize(event.getMessage());
        if (author.isEmpty() || content.isEmpty() || messages.size() >= 100) { return; }
        messages.add(Json.obj("author", author, "content", content, "systemMessageType", 1,
            "clanName", channel.getName(), "clanTitle", "", "timestamp", Instant.ofEpochSecond(event.getTimestamp())));
        if (messages.size() >= sync.state().limit("chatBatch", 20, 1, 20)) { flush(Instant.now()); }
        else if (messages.size() == 1) { due = Instant.now().plusSeconds(3); }
    }
    public static boolean accepts(ChatMessageType type, String channel, String clan)
    { return type == ChatMessageType.CLAN_CHAT && !Json.norm(clan).isEmpty() && Json.norm(channel).equals(Json.norm(clan)); }
    public void tick(Instant now)
    { if (!consent.allows("chat")) { reset(); } else if (!now.isBefore(due)) { flush(now); } }
    private void flush(Instant now)
    {
        if (messages.size() == 0 || !consent.allows("chat")) { return; }
        JsonArray batch = new JsonArray(); int limit = sync.state().limit("chatBatch", 20, 1, 20);
        while (messages.size() > 0 && batch.size() < limit) { batch.add(messages.remove(0)); }
        // Chat is short-lived. One batch remains in the common outbox, bounded by its 15-minute TTL.
        outbox.submit("chat", "/chat", Json.obj("messages", batch), null, ignored -> {}); due = now.plusSeconds(3);
    }
    public void reset() { messages = new JsonArray(); due = Instant.EPOCH; }
}
