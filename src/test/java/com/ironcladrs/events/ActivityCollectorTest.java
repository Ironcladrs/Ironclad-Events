package com.ironcladrs.events;

import com.google.gson.*;
import com.ironcladrs.events.activity.ActivityCollector;
import com.ironcladrs.events.queue.*;
import java.time.Instant;
import net.runelite.api.*;
import net.runelite.api.events.*;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ActivityCollectorTest
{
    @Test public void ownMessagesBatchOnceAndClanMessagesAreIgnored() throws Exception
    {
        ActivityCollector collector=new ActivityCollector(); Consent consent=mock(Consent.class); Outbox outbox=mock(Outbox.class); Client client=mock(Client.class);
        TestSupport.inject(collector,"client",client); TestSupport.inject(collector,"consent",consent); TestSupport.inject(collector,"outbox",outbox);
        when(consent.allows("clues")).thenReturn(true); when(outbox.submit(any(),any(),any(),isNull(),any())).thenReturn(true);
        ChatMessage event=mock(ChatMessage.class); when(event.getMessage()).thenReturn("You have completed 1,234 hard Treasure Trails.");
        when(event.getType()).thenReturn(ChatMessageType.CLAN_CHAT); collector.onChatMessage(event);collector.tick(Instant.now());verifyNoInteractions(outbox);
        when(event.getType()).thenReturn(ChatMessageType.GAMEMESSAGE);collector.onChatMessage(event);collector.onChatMessage(event);collector.tick(Instant.now().plusSeconds(4));
        ArgumentCaptor<JsonObject> payload=ArgumentCaptor.forClass(JsonObject.class);verify(outbox).submit(eq("clues"),eq("/activity"),payload.capture(),isNull(),any());
        JsonArray rows=payload.getValue().getAsJsonArray("events");assertEquals(1,rows.size());assertEquals(1234,rows.get(0).getAsJsonObject().get("count").getAsInt());
    }
    @Test public void loginLevelsDoNotAnnounceButAReal99Does() throws Exception
    {
        ActivityCollector collector=new ActivityCollector(); Consent consent=mock(Consent.class); Outbox outbox=mock(Outbox.class); Client client=mock(Client.class);
        TestSupport.inject(collector,"client",client); TestSupport.inject(collector,"consent",consent); TestSupport.inject(collector,"outbox",outbox);
        when(consent.allows("milestones")).thenReturn(true);when(client.getRealSkillLevel(any())).thenReturn(1);when(client.getRealSkillLevel(Skill.ATTACK)).thenReturn(98);
        Instant now=Instant.now();collector.tick(now);verifyNoInteractions(outbox);
        when(client.getRealSkillLevel(Skill.ATTACK)).thenReturn(99);collector.onStatChanged(new StatChanged(Skill.ATTACK,13034431,99,99));collector.tick(now.plusSeconds(4));
        verify(outbox).submit(eq("milestones"),eq("/activity"),any(),isNull(),any());
    }
}
