package com.ironcladrs.events;

import com.google.gson.JsonObject;
import com.ironcladrs.events.death.DeathListener;
import com.ironcladrs.events.capture.Screenshots;
import com.ironcladrs.events.queue.*;
import net.runelite.api.*;
import net.runelite.api.events.ActorDeath;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DeathListenerTest
{
    @Test public void onlyOwnDeathIsQueuedAndRepeatedActorEventsDoNotDuplicate() throws Exception
    {
        DeathListener listener = new DeathListener(); Client client = mock(Client.class); Player player = mock(Player.class);
        Consent consent = mock(Consent.class); Outbox outbox = mock(Outbox.class); Screenshots shots = mock(Screenshots.class);
        TestSupport.inject(listener,"client",client); TestSupport.inject(listener,"consent",consent); TestSupport.inject(listener,"outbox",outbox); TestSupport.inject(listener,"screenshots",shots);
        TestSupport.inject(listener,"config",new IroncladEventsConfig() { @Override public boolean autoDeaths(){return true;} });
        when(client.getLocalPlayer()).thenReturn(player); when(client.getTickCount()).thenReturn(10); when(consent.allows("deaths")).thenReturn(true);
        listener.onActorDeath(new ActorDeath(mock(NPC.class))); verifyNoInteractions(outbox);
        listener.onActorDeath(new ActorDeath(player)); listener.onActorDeath(new ActorDeath(player));
        ArgumentCaptor<JsonObject> payload = ArgumentCaptor.forClass(JsonObject.class);
        verify(outbox).submit(eq("deaths"),eq("/death"),payload.capture(),isNull(),any());
        assertEquals(1,payload.getValue().size()); assertTrue(payload.getValue().has("at")); verifyNoInteractions(shots);
        when(consent.allows("deaths")).thenReturn(false); when(client.getTickCount()).thenReturn(20); listener.onActorDeath(new ActorDeath(player)); verifyNoMoreInteractions(outbox);
    }
}
