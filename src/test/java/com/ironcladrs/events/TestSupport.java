package com.ironcladrs.events;

import com.google.gson.JsonArray;
import java.lang.reflect.Field;
import net.runelite.client.callback.ClientThread;
import static org.mockito.Mockito.*;

final class TestSupport
{
    static void inject(Object target, String name, Object value) throws Exception
    { Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value); }
    static PlayerGuard.Identity identity()
    { JsonArray worlds = new JsonArray(); worlds.add("MEMBERS"); return new PlayerGuard.Identity("Test Player", 123L, "ice_" + "a".repeat(43), worlds); }
    static ClientThread immediateThread()
    {
        ClientThread thread = mock(ClientThread.class);
        doAnswer(call -> { call.getArgument(0, Runnable.class).run(); return null; }).when(thread).invoke(any(Runnable.class));
        return thread;
    }
}
