package com.ironcladrs.events;

import com.ironcladrs.events.loot.*;
import com.ironcladrs.events.queue.Consent;
import java.util.*;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LootCollectorTest
{
    @Test public void identicalIdsStackWithoutRepricingOrCanonicalizing()
    {
        Map<Integer, Long> items = LootCollector.stack(Arrays.asList(new ItemStack(4151, 1), new ItemStack(4151, 2), new ItemStack(4152, 1)));
        assertEquals(Long.valueOf(3), items.get(4151)); assertEquals(Long.valueOf(1), items.get(4152));
    }
    @Test public void pvpAndDisabledLootNeverReachSubmitter() throws Exception
    {
        LootCollector collector = new LootCollector(); Consent consent = mock(Consent.class); LootSubmitter submitter = mock(LootSubmitter.class);
        TestSupport.inject(collector, "consent", consent); TestSupport.inject(collector, "submitter", submitter);
        LootReceived pvp = mock(LootReceived.class); when(pvp.getType()).thenReturn(LootRecordType.PLAYER);
        when(consent.allows("loot")).thenReturn(true); collector.onLootReceived(pvp);
        when(consent.allows("loot")).thenReturn(false); collector.onLootReceived(mock(LootReceived.class));
        verifyNoInteractions(submitter);
    }

    @Test public void npcRaidAndCasketUseTheSameSerializedItemsAndFreshKc() throws Exception
    {
        LootCollector collector = new LootCollector(); Consent consent = mock(Consent.class); LootSubmitter submitter = mock(LootSubmitter.class);
        net.runelite.api.Client client = mock(net.runelite.api.Client.class); net.runelite.client.game.ItemManager items = mock(net.runelite.client.game.ItemManager.class);
        com.ironcladrs.events.progress.KillCountCollector kc = new com.ironcladrs.events.progress.KillCountCollector();
        TestSupport.inject(collector,"consent",consent); TestSupport.inject(collector,"submitter",submitter); TestSupport.inject(collector,"client",client); TestSupport.inject(collector,"items",items); TestSupport.inject(collector,"kc",kc);
        when(consent.allows("loot")).thenReturn(true); when(client.getTickCount()).thenReturn(10);
        net.runelite.api.ItemComposition item = mock(net.runelite.api.ItemComposition.class); when(item.getMembersName()).thenReturn("Abyssal whip"); when(items.getItemComposition(4151)).thenReturn(item); when(items.getItemPrice(4151)).thenReturn(123L);
        net.runelite.api.NPCComposition npc=mock(net.runelite.api.NPCComposition.class); when(npc.getName()).thenReturn("Sol Heredit"); when(npc.getId()).thenReturn(12821);
        kc.accept("Your Sol Heredit kill count is: 14.",9);
        collector.onServerNpcLoot(new net.runelite.client.events.ServerNpcLoot(npc,Arrays.asList(new ItemStack(4151,1),new ItemStack(4151,2))));
        collector.onLootReceived(new LootReceived("Chambers of Xeric",0,LootRecordType.EVENT,Collections.singletonList(new ItemStack(4151,3)),1,null));
        collector.onLootReceived(new LootReceived("Clue Scroll (Master)",0,LootRecordType.EVENT,Collections.singletonList(new ItemStack(4151,3)),1,null));
        org.mockito.ArgumentCaptor<com.google.gson.JsonObject> sources=org.mockito.ArgumentCaptor.forClass(com.google.gson.JsonObject.class);
        org.mockito.ArgumentCaptor<com.google.gson.JsonArray> data=org.mockito.ArgumentCaptor.forClass(com.google.gson.JsonArray.class);
        verify(submitter,times(3)).submit(sources.capture(),data.capture(),any());
        assertEquals(14,sources.getAllValues().get(0).get("killCount").getAsInt());
        assertEquals(data.getAllValues().get(0),data.getAllValues().get(1));assertEquals(data.getAllValues().get(0),data.getAllValues().get(2));
    }
}
