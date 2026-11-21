package com.ironcladrs.events;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.sync.SessionState;
import com.ironcladrs.events.ui.*;
import java.awt.*;
import java.time.Instant;
import java.util.*;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Exercise the actual RuneLite wrapper, not an unrelated Swing scroll fixture. */
public class PanelScrollTest
{
    private static void layout(Component component)
    { if (component instanceof Container) { ((Container) component).doLayout(); for (Component child : ((Container) component).getComponents()) { layout(child); } } }
    private static <T> T find(Component component, Class<T> type)
    {
        if (type.isInstance(component)) { return type.cast(component); }
        if (component instanceof Container) { for (Component child : ((Container) component).getComponents()) { T found = find(child, type); if (found != null) { return found; } } }
        return null;
    }
    private static java.util.List<JButton> syncButtons(Component component)
    {
        java.util.List<JButton> result = new ArrayList<>();
        if (component instanceof JButton && "Sync now".equals(((JButton) component).getText())) { result.add((JButton) component); }
        if (component instanceof Container) { for (Component child : ((Container) component).getComponents()) { result.addAll(syncButtons(child)); } }
        return result;
    }
    @Test public void aLongEventScrollsToTheFooterAtSidebarWidthAndSyncIsAvailableAtTheTop() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            IroncladPanel.Actions actions = mock(IroncladPanel.Actions.class);
            IroncladPanel panel = new IroncladPanel(new IroncladEventsConfig() {}, null, actions);
            try
            {
                JsonArray tasks = new JsonArray();
                for (int i = 0; i < 100; i++) { tasks.add(Json.obj("id", "t:" + i, "label", "Task " + i, "category", "Test objectives", "progressText", "your team 0 of 1", "url", "https://ironcladrs.com/events/1")); }
                JsonArray events = new JsonArray(); events.add(Json.obj("id", 1, "name", "Scrolling regression", "typeLabel", "Bingo", "state", "live", "tasks", tasks));
                panel.update(new SessionState(true, "Connected", Instant.now(), new JsonObject(), Json.obj("events", events)), 0, actions);
                JPanel wrapper = panel.getWrappedPanel(); wrapper.setSize(242, 500); layout(wrapper);
                JScrollPane scroll = find(wrapper, JScrollPane.class); assertNotNull("RuneLite must supply the sidebar scroll container", scroll);
                assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, scroll.getHorizontalScrollBarPolicy());
                JViewport viewport = scroll.getViewport();
                assertTrue("Long content exceeds the visible sidebar", viewport.getViewSize().height > viewport.getExtentSize().height);
                java.util.List<JButton> buttons = syncButtons(panel); assertEquals("Sync at both ends", 2, buttons.size());
                Rectangle top = SwingUtilities.convertRectangle(buttons.get(0).getParent(), buttons.get(0).getBounds(), (Container) viewport.getView());
                assertTrue("Top sync is initially visible", viewport.getViewRect().contains(top));
                buttons.get(0).doClick(); verify(actions).sync();
                StatusFooter footer = find(panel, StatusFooter.class); assertNotNull(footer);
                Rectangle bottom = SwingUtilities.convertRectangle(footer.getParent(), footer.getBounds(), (Container) viewport.getView());
                scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum()); layout(wrapper);
                assertTrue("Footer can be reached", viewport.getViewRect().contains(bottom));
                buttons.get(1).doClick(); verify(actions, times(2)).sync();
            }
            finally { panel.stop(); }
        });
    }
}
