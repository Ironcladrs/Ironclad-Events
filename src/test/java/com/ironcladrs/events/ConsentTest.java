package com.ironcladrs.events;

import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.queue.Consent;
import com.ironcladrs.events.sync.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ConsentTest
{
    @Test public void deathOptInWorksWithoutDropsButNeverWithoutMasterConnection() throws Exception
    {
        Consent consent=new Consent(); PlayerGuard guard=mock(PlayerGuard.class); SyncService sync=mock(SyncService.class);
        TestSupport.inject(consent,"guard",guard); TestSupport.inject(consent,"sync",sync);
        when(guard.identity()).thenReturn(TestSupport.identity());when(sync.current()).thenReturn(true);
        when(sync.state()).thenReturn(new SessionState(true,"Connected",null,Json.obj("features",Json.obj("deaths",true,"loot",true)),Json.obj()));
        TestSupport.inject(consent,"config",new IroncladEventsConfig(){@Override public boolean connect(){return true;} @Override public boolean autoDeaths(){return true;}});
        assertTrue(consent.allows("deaths"));assertFalse(consent.allows("loot"));
        TestSupport.inject(consent,"config",new IroncladEventsConfig(){@Override public boolean autoDeaths(){return true;}});
        assertFalse(consent.allows("deaths"));
    }
}
