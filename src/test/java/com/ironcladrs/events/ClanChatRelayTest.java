package com.ironcladrs.events;

import com.ironcladrs.events.chat.*;
import net.runelite.api.ChatMessageType;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClanChatRelayTest
{
    @Test public void onlyTypedOwnClanChatIsAccepted()
    {
        assertTrue(ClanChatRelay.accepts(ChatMessageType.CLAN_CHAT, "Ironclad", "ironclad"));
        assertFalse(ClanChatRelay.accepts(ChatMessageType.CLAN_CHAT, "Other clan", "Ironclad"));
        for (ChatMessageType type : new ChatMessageType[]{ChatMessageType.CLAN_GUEST_CHAT, ChatMessageType.CLAN_MESSAGE, ChatMessageType.PRIVATECHAT, ChatMessageType.PUBLICCHAT, ChatMessageType.FRIENDSCHAT})
        { assertFalse(type.name(), ClanChatRelay.accepts(type, "Ironclad", "Ironclad")); }
        assertFalse(ClanChatRelay.accepts(ChatMessageType.CLAN_CHAT, "", ""));
    }
    @Test public void tagsTemplatesAndLengthAreSanitized()
    {
        assertEquals("Alice: hello", MessageSanitizer.sanitize("<img=2>Alice: <col=ff0000>hello</col>"));
        assertEquals("Alice completed The Lost Tribe", MessageSanitizer.sanitize("<str_quest_name_0=The Lost Tribe>Alice completed <str_quest_name_0>"));
        assertEquals(500, MessageSanitizer.sanitize("a".repeat(600)).length());
    }
}
