package com.ironcladrs.events.ui;

import com.ironcladrs.events.sync.SessionState;
import java.awt.Component;
import java.time.*;
import javax.swing.*;

/** The timer updates this small label, never rebuilds event/task lists per frame. */
public final class StatusFooter extends JPanel
{
    private final JLabel label;
    private SessionState state;
    private int unsent;
    public StatusFooter(SessionState state, int unsent, Runnable sync)
    {
        this.state = state; this.unsent = unsent; setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT);
        setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        label = Ui.muted("", Ui.WIDTH); add(label); update();
        add(Ui.buttons(Ui.button("Sync now", sync), Ui.button("What is sent", () -> net.runelite.client.util.LinkBrowser.browse("https://ironcladrs.com/privacy#plugin"))));
    }
    public void state(SessionState state) { this.state = state; update(); }
    public void unsent(int count) { unsent = count; update(); }
    public void update()
    {
        String synced = state.syncedAt == null ? "Not synced yet" : "Synced " + Ui.ago(state.syncedAt, Instant.now());
        label.setText("<html>" + synced + (unsent > 0 ? "<br>" + unsent + " waiting to send" : "") + "</html>");
    }
}
