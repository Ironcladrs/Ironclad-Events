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
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;

/** The clan channel as Clan Chat Webhooks (clanchat.net) relays it: members' messages and the clan's
 * system broadcasts, with the same message-type codes. Guest, friends, public and private chat never
 * enter the buffer. Nothing is sent unless Connect and Relay clan chat are both on. */
@Singleton
public class ClanChatRelay
{
    @Inject private Client client;
    @Inject private Consent consent;
    @Inject private Outbox outbox;
    @Inject private SyncService sync;
    @Inject private com.ironcladrs.events.IroncladEventsConfig config;
    private JsonArray messages = new JsonArray();
    private Instant due = Instant.EPOCH;
    @Subscribe public void onChatMessage(ChatMessage event)
    {
        if (!consent.allows("chat")) { return; }
        ClanChannel channel = client.getClanChannel();
        if (channel == null || !accepts(event.getType(), channel.getName(), sync.state().clanName())) { return; }
        String content = MessageSanitizer.sanitize(event.getMessage());
        int type = systemMessageType(event.getType(), content);
        // A member's message needs its author; the "how to talk in clan chat" login line is never sent.
        String author = MessageSanitizer.sanitize(event.getName()).trim();
        if (content.isEmpty() || type == LOGIN || (type == NORMAL && author.isEmpty()) || messages.size() >= 100) { return; }
        if (type != NORMAL && !new com.ironcladrs.events.filters.Filters(config, sync.state()).relayBroadcasts) { return; }
        messages.add(Json.obj("author", type == NORMAL ? author : "", "content", content, "systemMessageType", type,
            "accountType", accountType(event.getName()), "clanName", channel.getName(), "clanTitle", type == NORMAL ? rankTitle(author) : "",
            "timestamp", Instant.ofEpochSecond(event.getTimestamp())));
        if (messages.size() >= sync.state().limit("chatBatch", 20, 1, 20)) { flush(Instant.now()); }
        else if (messages.size() == 1) { due = Instant.now().plusSeconds(3); }
    }
    public static boolean accepts(ChatMessageType type, String channel, String clan)
    {
        return (type == ChatMessageType.CLAN_CHAT || type == ChatMessageType.CLAN_MESSAGE)
            && !Json.norm(clan).isEmpty() && Json.norm(channel).equals(Json.norm(clan));
    }
    // ── Message types, as Clan Chat Webhooks codes them ──────────────────────
    static final int NORMAL = 1, LOGIN = -1, UNKNOWN = 100;
    public static int systemMessageType(ChatMessageType type, String message)
    {
        if (type != ChatMessageType.CLAN_MESSAGE) { return NORMAL; }
        if (message.contains("received a drop:")) { return 2; }
        if (message.contains("received special loot from a raid:")) { return 3; }
        if (message.contains("has completed a quest:")) { return 7; }
        if (message.contains("received a new collection log item:")) { return 6; }
        if (message.contains("personal best:")) { return 5; }
        if (message.contains("To talk in your clan's channel, start each line of chat with")) { return LOGIN; }
        if (message.contains("has defeated") || message.contains("has been defeated by")) { return 8; }
        if (message.contains("has a funny feeling like") || message.contains("backpack:") || message.contains("something special:")) { return 4; }
        if ((message.contains("has reached") && (message.contains("level") || message.contains("XP"))) || message.contains("has reached a total level of")) { return 10; }
        if (message.contains("tier of rewards from Combat Achievements!") || (message.contains("has completed") && message.contains("combat task"))) { return 11; }
        if (message.contains("received a clue item:")) { return 12; }
        if (message.contains("has left.") || message.contains("has been invited into the clan by") || message.contains("has joined.")) { return 9; }
        if (message.contains("has completed the") && message.contains("diary.")) { return 13; }
        return UNKNOWN;
    }
    // The account-type icon in front of a sender's name (same codes as Clan Chat Webhooks).
    public static int accountType(String name)
    {
        String n = name == null ? "" : name;
        if (n.contains("<img=0>")) { return 8; }
        if (n.contains("<img=2>")) { return 2; }
        if (n.contains("<img=10>")) { return 3; }
        if (n.contains("<img=3>")) { return 4; }
        if (n.contains("<img=41>")) { return 6; }
        if (n.contains("<img=43>")) { return 5; }
        if (n.contains("<img=42>")) { return 7; }
        return 1;
    }
    private String rankTitle(String author)
    {
        ClanChannel channel = client.getClanChannel();
        ClanSettings settings = client.getClanSettings();
        ClanChannelMember member = channel == null ? null : channel.findMember(author);
        ClanTitle title = member == null || settings == null ? null : settings.titleForRank(member.getRank());
        return title == null || title.getName() == null ? "" : title.getName();
    }
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
