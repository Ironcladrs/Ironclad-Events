package com.ironcladrs.events;

import com.ironcladrs.events.account.AccountSnapshot;
import com.ironcladrs.events.api.ApiClient;
import com.ironcladrs.events.capture.Screenshots;
import com.ironcladrs.events.chat.ClanChatRelay;
import com.ironcladrs.events.clog.*;
import com.ironcladrs.events.pb.PersonalBestListener;
import com.ironcladrs.events.precheck.BankAttestor;
import com.ironcladrs.events.progress.*;
import com.ironcladrs.events.queue.Outbox;
import com.ironcladrs.events.sync.SyncService;
import java.lang.reflect.Method;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class LifecycleTest
{
    @Test public void identityChangeObservedByCollectorStillClearsEveryBufferBeforeNextPulse() throws Exception
    {
        IroncladEventsPlugin plugin = new IroncladEventsPlugin(); PlayerGuard guard = mock(PlayerGuard.class);
        when(guard.generation()).thenReturn(2L); when(guard.refresh()).thenReturn(false);
        TestSupport.inject(plugin, "guard", guard); TestSupport.inject(plugin, "activeGeneration", 1L);
        TestSupport.inject(plugin, "api", mock(ApiClient.class)); TestSupport.inject(plugin, "sync", mock(SyncService.class));
        TestSupport.inject(plugin, "outbox", mock(Outbox.class)); TestSupport.inject(plugin, "screenshots", mock(Screenshots.class));
        TestSupport.inject(plugin, "bank", mock(BankAttestor.class)); TestSupport.inject(plugin, "xp", mock(XpCollector.class));
        TestSupport.inject(plugin, "kc", mock(KillCountCollector.class)); TestSupport.inject(plugin, "progress", mock(ProgressBatcher.class));
        TestSupport.inject(plugin, "collection", mock(CollectionLogReader.class)); TestSupport.inject(plugin, "newItems", mock(NewItemListener.class));
        TestSupport.inject(plugin, "deaths", mock(com.ironcladrs.events.death.DeathListener.class)); TestSupport.inject(plugin, "activity", mock(com.ironcladrs.events.activity.ActivityCollector.class));
        TestSupport.inject(plugin, "pbs", mock(PersonalBestListener.class)); TestSupport.inject(plugin, "chat", mock(ClanChatRelay.class));
        AccountSnapshot account = mock(AccountSnapshot.class); TestSupport.inject(plugin, "account", account);
        Method pulse = IroncladEventsPlugin.class.getDeclaredMethod("pulse"); pulse.setAccessible(true); pulse.invoke(plugin);
        verify(account).reset(); verify(account).tick(any());
    }
}
