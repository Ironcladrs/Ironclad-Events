package com.ironcladrs.events;

import com.google.gson.JsonObject;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.sync.SessionState;
import com.ironcladrs.events.ui.*;
import java.awt.Component;
import java.time.Instant;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ClanSettingsCardTest
{
    @Test public void leavingClanSettingsChangesOnlyTheLocalPresetChoice() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            IroncladPanel.Actions actions = mock(IroncladPanel.Actions.class);
            IroncladEventsConfig config = new IroncladEventsConfig() { @Override public boolean useClanSettings() { return true; } };
            SessionState state = new SessionState(true, "", Instant.now(), new JsonObject(), Json.obj("clanPreset", Json.obj("version", 1, "revision", "test")));
            ClanSettingsCard card = new ClanSettingsCard(config, state, actions); boolean clicked = false;
            for (Component component : card.getComponents())
            { if (component instanceof JButton && ((JButton) component).getText().equals("Use personal settings")) { ((JButton) component).doClick(); clicked = true; } }
            assertTrue(clicked); verify(actions).clanSettings(false); verifyNoMoreInteractions(actions);
            // The card changes no upload switch itself: they keep their defaults.
            assertTrue(config.connect()); assertTrue(config.autoDrops()); assertTrue(config.deathScreenshots());
        });
    }
    @Test public void clanDefaultsAsksOnlyForMissingRecommendedUploadsAndShowsTheWarning() throws Exception
    {
        IroncladEventsConfig config = new IroncladEventsConfig() { @Override public boolean autoDrops() { return true; } @Override public boolean syncCollectionLog() { return false; } };
        JsonObject preset = Json.obj("version", 1, "revision", "r1", "recommended", Json.obj("autoDrops", true, "syncCollectionLog", true, "relayClanChat", false, "notAKey", true));
        java.util.List<Integer> missing = ClanSettingsCard.missing(config, preset);
        assertEquals(1, missing.size()); assertEquals("syncCollectionLog", IroncladEventsConfig.UPLOAD_KEYS[missing.get(0)]);
        String text = ClanSettingsCard.confirmText(config, preset);
        assertTrue(text.startsWith(IroncladEventsConfig.THIRD_PARTY_WARNING)); assertTrue(text.contains("- Collection log"));
        assertFalse(text.contains("Event tile drops")); assertFalse(text.contains("Clan chat relay"));
    }
}
