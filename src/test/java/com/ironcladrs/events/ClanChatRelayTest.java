package com.ironcladrs.events;

import com.ironcladrs.events.chat.*;
import net.runelite.api.ChatMessageType;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClanChatRelayTest
{
    @Test public void onlyOwnClanChannelIsAccepted()
    {
        assertTrue(ClanChatRelay.accepts(ChatMessageType.CLAN_CHAT, "Ironclad", "ironclad"));
        assertTrue("clan broadcasts, as Clan Chat Webhooks relays them", ClanChatRelay.accepts(ChatMessageType.CLAN_MESSAGE, "Ironclad", "Ironclad"));
        assertFalse(ClanChatRelay.accepts(ChatMessageType.CLAN_CHAT, "Other clan", "Ironclad"));
        for (ChatMessageType type : new ChatMessageType[]{ChatMessageType.CLAN_GUEST_CHAT, ChatMessageType.CLAN_GUEST_MESSAGE, ChatMessageType.PRIVATECHAT, ChatMessageType.PUBLICCHAT, ChatMessageType.FRIENDSCHAT})
        { assertFalse(type.name(), ClanChatRelay.accepts(type, "Ironclad", "Ironclad")); }
        assertFalse(ClanChatRelay.accepts(ChatMessageType.CLAN_CHAT, "", ""));
    }
    @Test public void broadcastsAndIconsUseClanChatWebhooksCodes()
    {
        assertEquals(1, ClanChatRelay.systemMessageType(ChatMessageType.CLAN_CHAT, "anything"));
        assertEquals(2, ClanChatRelay.systemMessageType(ChatMessageType.CLAN_MESSAGE, "Alice received a drop: Dragon claws"));
        assertEquals(6, ClanChatRelay.systemMessageType(ChatMessageType.CLAN_MESSAGE, "Alice received a new collection log item: Vorki"));
        assertEquals(-1, ClanChatRelay.systemMessageType(ChatMessageType.CLAN_MESSAGE, "To talk in your clan's channel, start each line of chat with //"));
        assertEquals(100, ClanChatRelay.systemMessageType(ChatMessageType.CLAN_MESSAGE, "Something new"));
        assertEquals(2, ClanChatRelay.accountType("<img=2>Iron Al"));
        assertEquals(1, ClanChatRelay.accountType("Main Al"));
    }
    @Test public void tagsTemplatesAndLengthAreSanitized()
    {
        assertEquals("Alice: hello", MessageSanitizer.sanitize("<img=2>Alice: <col=ff0000>hello</col>"));
        assertEquals("Alice completed The Lost Tribe", MessageSanitizer.sanitize("<str_quest_name_0=The Lost Tribe>Alice completed <str_quest_name_0>"));
        assertEquals(500, MessageSanitizer.sanitize("a".repeat(600)).length());
    }
}
