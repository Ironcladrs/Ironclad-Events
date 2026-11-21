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
            assertFalse(config.connect()); assertFalse(config.autoDrops()); assertFalse(config.deathScreenshots());
        });
    }
}
