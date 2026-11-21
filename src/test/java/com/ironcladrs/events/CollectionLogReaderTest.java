package com.ironcladrs.events;

import com.ironcladrs.events.clog.CollectionLogReader;
import com.ironcladrs.events.queue.Consent;
import net.runelite.api.Client;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CollectionLogReaderTest
{
    @Test public void anotherPlayersLogNeverTriggersSearch() throws Exception
    {
        CollectionLogReader reader = new CollectionLogReader(); Client client = mock(Client.class); Consent consent = mock(Consent.class);
        TestSupport.inject(reader, "client", client); TestSupport.inject(reader, "consent", consent);
        when(consent.allows("account")).thenReturn(true); when(client.getVarbitValue(VarbitID.COLLECTION_POH_HOST_BOOK_OPEN)).thenReturn(1);
        ScriptPostFired event = new ScriptPostFired(7797); reader.onScriptPostFired(event);
        verify(client, never()).runScript(anyInt(), any()); assertFalse(CollectionLogReader.ownLog(1)); assertTrue(CollectionLogReader.ownLog(0));
        verify(client, never()).menuAction(anyInt(), anyInt(), any(), anyInt(), anyInt(), any(), any());
    }
}
