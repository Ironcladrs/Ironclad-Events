package com.ironcladrs.events;

import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.ui.BankSendCard;
import com.ironcladrs.events.ui.IroncladPanel;
import com.ironcladrs.events.ui.Ui;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import javax.swing.AbstractButton;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** The bank card builds (a switch once crashed it, 2026-11-27) and its switch decides "send automatically". */
public class BankSendCardTest
{
    private static <T> T find(Container root, Class<T> type, String text)
    {
        for (Component c : root.getComponents())
        {
            if (type.isInstance(c) && (text == null || (c instanceof AbstractButton && text.equals(((AbstractButton) c).getText())))) { return type.cast(c); }
            if (c instanceof Container) { T inner = find((Container) c, type, text); if (inner != null) { return inner; } }
        }
        return null;
    }

    @Test public void switchOnThenSendSendsAutomatically()
    {
        IroncladPanel.Actions actions = mock(IroncladPanel.Actions.class);
        BankSendCard card = new BankSendCard(Json.obj("eventId", 4, "precheckId", 7, "label", "Plugin bank check", "counts", Json.obj("526", 0), "names", Json.obj("526", "Bones")), null, actions);
        find(card, AbstractButton.class, "Send").doClick();
        verify(actions).bank(4, 7, false);
        Ui.Switch toggle = find(card, Ui.Switch.class, null);
        assertNotNull(toggle);
        for (java.awt.event.MouseListener l : toggle.getMouseListeners()) { l.mouseClicked(new MouseEvent(toggle, MouseEvent.MOUSE_CLICKED, 0, 0, 1, 1, 1, false)); }
        find(card, AbstractButton.class, "Send").doClick();
        verify(actions).bank(4, 7, true);
    }
}
