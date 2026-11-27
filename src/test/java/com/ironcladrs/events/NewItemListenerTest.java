package com.ironcladrs.events;

import com.google.gson.JsonObject;
import com.ironcladrs.events.account.AccountSnapshot;
import com.ironcladrs.events.clog.NewItemListener;
import com.ironcladrs.events.queue.Consent;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NewItemListenerTest
{
    @Test public void newSlotsResolveOnlyAgainstCollectionItemsNotOtherSameNamedDefinitions() throws Exception
    {
        NewItemListener listener = new NewItemListener(); Client client = mock(Client.class);
        ItemManager items = mock(ItemManager.class); AccountSnapshot account = mock(AccountSnapshot.class); Consent consent = mock(Consent.class);
        TestSupport.inject(listener,"client",client); TestSupport.inject(listener,"items",items);
        TestSupport.inject(listener,"account",account); TestSupport.inject(listener,"consent",consent); when(consent.accountPart("collectionLog")).thenReturn(true);
        EnumComposition tabs = mock(EnumComposition.class), pages = mock(EnumComposition.class), entries = mock(EnumComposition.class);
        StructComposition tab = mock(StructComposition.class), page = mock(StructComposition.class);
        when(client.getEnum(2102)).thenReturn(tabs); when(tabs.getIntVals()).thenReturn(new int[]{10}); when(client.getStructComposition(10)).thenReturn(tab);
        when(tab.getIntValue(683)).thenReturn(20); when(client.getEnum(20)).thenReturn(pages); when(pages.getIntVals()).thenReturn(new int[]{30});
        when(client.getStructComposition(30)).thenReturn(page); when(page.getIntValue(690)).thenReturn(40); when(client.getEnum(40)).thenReturn(entries); when(entries.getIntVals()).thenReturn(new int[]{100});
        ItemComposition item = mock(ItemComposition.class); when(items.getItemComposition(100)).thenReturn(item);
        when(item.getId()).thenReturn(100); when(item.getName()).thenReturn("Unique item"); when(item.getMembersName()).thenReturn("Unique item");
        when(item.getNote()).thenReturn(-1); when(item.getPlaceholderTemplateId()).thenReturn(-1); when(items.canonicalize(100)).thenReturn(100);
        ChatMessage message = mock(ChatMessage.class); when(message.getType()).thenReturn(ChatMessageType.GAMEMESSAGE); when(message.getMessage()).thenReturn("New item added to your collection log: Unique item");
        listener.onChatMessage(message); listener.onGameTick(new GameTick());
        ArgumentCaptor<JsonObject> payload = ArgumentCaptor.forClass(JsonObject.class); verify(account).part(eq("collectionLog"),payload.capture());
        assertEquals(1,payload.getValue().getAsJsonObject("items").get("100").getAsInt());
        assertFalse(payload.getValue().get("full").getAsBoolean()); verify(client,never()).getItemCount(); verify(items,never()).getItemComposition(1);
    }
}
