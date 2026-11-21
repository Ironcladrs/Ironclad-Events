package com.ironcladrs.events.ui;

import com.ironcladrs.events.sync.SessionState;
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
        this.state = state; this.unsent = unsent; setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false);
        label = Ui.text(""); add(label); update(); add(Ui.button("Sync now", sync)); Ui.link(this, "What is sent", "https://ironcladrs.com/privacy#plugin");
    }
    public void state(SessionState state) { this.state = state; update(); }
    public void unsent(int count) { unsent = count; update(); }
    public void update()
    { label.setText("<html>" + (state.syncedAt == null ? "Not synced" : "Synced " + Math.max(0, Duration.between(state.syncedAt, Instant.now()).getSeconds()) + " s ago") + (unsent > 0 ? "<br>" + unsent + " unsent" : "") + "</html>"); }
}
