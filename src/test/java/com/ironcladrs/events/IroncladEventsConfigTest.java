package com.ironcladrs.events;

import java.lang.reflect.Method;
import java.util.*;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;
import static org.junit.Assert.*;

public class IroncladEventsConfigTest
{
    private final IroncladEventsConfig config = new IroncladEventsConfig() {};

    @Test
    public void everyUploadStartsOnLikeClanSocket()
    {
        // User, 2026-11-27: "everything starts on like clan socket". Nothing is sent until the account is linked.
        assertFalse(config.useClanSettings());
        assertTrue(config.connect());
        for (String key : IroncladEventsConfig.UPLOAD_KEYS) { assertTrue(key, IroncladEventsConfig.upload(config, key)); }
        assertEquals("", config.apiKey());
    }

    /** One gate: Connect carries RuneLite's third-party warning and every upload key is a real setting. */
    @Test
    public void connectIsTheOnlyWarnedSwitchAndEveryUploadKeyExists()
    {
        Set<String> keys = new HashSet<>(), warned = new HashSet<>();
        for (Method method : IroncladEventsConfig.class.getMethods())
        {
            ConfigItem item = method.getAnnotation(ConfigItem.class); if (item == null) { continue; }
            keys.add(item.keyName()); if (!item.warning().isEmpty()) { warned.add(item.keyName()); }
            assertTrue(item.keyName() + " name fits RuneLite's width", item.name().length() <= 25);
        }
        assertEquals(Collections.singleton("connect"), warned);
        assertEquals(IroncladEventsConfig.UPLOAD_KEYS.length, IroncladEventsConfig.UPLOAD_LABELS.length);
        for (String key : IroncladEventsConfig.UPLOAD_KEYS) { assertTrue(key, keys.contains(key)); }
    }

    @Test
    public void uploadReadsEachSwitchAndUnknownKeysAreOff()
    {
        for (String on : IroncladEventsConfig.UPLOAD_KEYS)
        {
            IroncladEventsConfig one = (IroncladEventsConfig) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IroncladEventsConfig.class},
                (proxy, method, args) -> method.getReturnType() == boolean.class ? on.equals(method.getAnnotation(ConfigItem.class) == null ? "" : method.getAnnotation(ConfigItem.class).keyName())
                    : method.getReturnType() == int.class ? 0 : "");
            for (String key : IroncladEventsConfig.UPLOAD_KEYS) { assertEquals(on + "/" + key, on.equals(key), IroncladEventsConfig.upload(one, key)); }
        }
        assertFalse(IroncladEventsConfig.upload(config, "apiKey"));
    }
}
