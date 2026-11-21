package com.ironcladrs.events;

import org.junit.Test;
import static org.junit.Assert.*;

public class IroncladEventsConfigTest
{
    private final IroncladEventsConfig config = new IroncladEventsConfig() {};

    @Test
    public void allUploadConsentStartsOff()
    {
        assertFalse(config.useClanSettings());
        assertFalse(config.connect());
        assertFalse(config.autoDrops());
        assertFalse(config.screenshots());
        assertFalse(config.bankPrechecks());
        assertFalse(config.liveProgress());
        assertFalse(config.syncAccount());
        assertFalse(config.autoPbs());
        assertFalse(config.relayClanChat());
        assertFalse(config.autoDeaths()); assertFalse(config.deathScreenshots());
        assertFalse(config.sharePets()); assertFalse(config.shareMilestones()); assertFalse(config.shareClues());
        assertEquals("", config.apiKey());
    }
}
