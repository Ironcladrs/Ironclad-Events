package com.ironcladrs.events;

import com.ironcladrs.events.capture.Screenshots;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScreenshotsTest
{
    @Test public void jpegIsInMemoryScaledAndNeverOverLimit() throws Exception
    {
        byte[] bytes = Screenshots.encode(new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB), 1500000);
        assertNotNull(bytes); assertEquals(1600, ImageIO.read(new ByteArrayInputStream(bytes)).getWidth());
        assertNull(Screenshots.encode(new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB), 1));
    }

    @Test public void missingFrameFallsBackOnceAndCancelsItsTimeout() throws Exception
    {
        Screenshots capture = new Screenshots(); PlayerGuard guard = org.mockito.Mockito.mock(PlayerGuard.class);
        org.mockito.Mockito.when(guard.identity()).thenReturn(TestSupport.identity()); org.mockito.Mockito.when(guard.generation()).thenReturn(1L);
        com.ironcladrs.events.capture.WorkExecutor work=org.mockito.Mockito.mock(com.ironcladrs.events.capture.WorkExecutor.class);
        java.util.concurrent.ScheduledFuture<?> timeout=org.mockito.Mockito.mock(java.util.concurrent.ScheduledFuture.class);
        final Runnable[] deadline=new Runnable[1];
        org.mockito.Mockito.doAnswer(call->{deadline[0]=call.getArgument(0);return timeout;}).when(work).schedule(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq(5L));
        com.ironcladrs.events.sync.SyncService sync=org.mockito.Mockito.mock(com.ironcladrs.events.sync.SyncService.class);
        org.mockito.Mockito.when(sync.state()).thenReturn(com.ironcladrs.events.sync.SessionState.empty("Test"));
        TestSupport.inject(capture,"guard",guard);TestSupport.inject(capture,"work",work);TestSupport.inject(capture,"sync",sync);TestSupport.inject(capture,"config",new IroncladEventsConfig(){});
        TestSupport.inject(capture,"clientThread",TestSupport.immediateThread());TestSupport.inject(capture,"drawManager",new net.runelite.client.ui.DrawManager());
        java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
        capture.capture(bytes->{assertNull(bytes);calls.incrementAndGet();});deadline[0].run();deadline[0].run();assertEquals(1,calls.get());
        org.mockito.Mockito.verify(timeout).cancel(false);
    }
}
