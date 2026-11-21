package com.ironcladrs.events.ui;

import com.ironcladrs.events.IroncladEventsConfig;
import com.ironcladrs.events.sync.AccountLinker;
import java.awt.*;
import javax.swing.*;
import net.runelite.client.ui.FontManager;

/**
 * The easy way in: press Link my account, approve the code on the site, and the plugin saves its own
 * key. Linking talks to ironcladrs.com, so it waits until Connect is on and its warning was accepted.
 */
public final class LinkCard extends JPanel
{
    public LinkCard(IroncladEventsConfig config, AccountLinker.State link, boolean linked, IroncladPanel.Actions actions)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT);
        add(Ui.bold(linked ? "Link again" : "Link your account", Ui.CARD_WIDTH));
        if (!config.connect())
        {
            add(Ui.text("1. Open this plugin's settings and turn on Connect to ironcladrs.com.", Ui.CARD_WIDTH));
            add(Ui.text("2. Come back here and press Link my account.", Ui.CARD_WIDTH));
            JButton button = Ui.button("Link my account", () -> {}); button.setEnabled(false); add(Ui.fit(button));
        }
        else if (link.phase == AccountLinker.State.Phase.WAITING)
        {
            add(Ui.muted("Your code", Ui.CARD_WIDTH));
            JLabel code = new JLabel(Ui.plain(link.code)); code.setFont(FontManager.getRunescapeBoldFont().deriveFont(22f)); code.setForeground(Ui.ACCENT);
            code.setAlignmentX(Component.LEFT_ALIGNMENT); code.getAccessibleContext().setAccessibleName("Link code " + link.code); add(code);
            add(Ui.text(link.message, Ui.CARD_WIDTH));
            add(Ui.muted("Check the site shows the same code, then press Link my account there.", Ui.CARD_WIDTH));
            Ui.link(this, "Open the page again", link.url);
            add(Ui.fit(Ui.button("Cancel", actions::cancelLink)));
        }
        else if (link.phase == AccountLinker.State.Phase.STARTING)
        { add(Ui.muted(link.message, Ui.CARD_WIDTH)); add(Ui.fit(Ui.button("Cancel", actions::cancelLink))); }
        else if (link.phase == AccountLinker.State.Phase.LINKED)
        { JLabel done = Ui.text(link.message, Ui.CARD_WIDTH); done.setForeground(Ui.GOOD); add(done); }
        else
        {
            if (link.phase == AccountLinker.State.Phase.FAILED) { JLabel error = Ui.text(link.message, Ui.CARD_WIDTH); error.setForeground(Ui.BAD); add(error); }
            add(Ui.muted("Opens ironcladrs.com in your browser. Sign in, approve the code, and the plugin saves your key itself.", Ui.CARD_WIDTH));
            add(Ui.fit(Ui.button("Link my account", actions::link)));
        }
        Ui.gap(this, 6);
        add(Ui.muted("Prefer to paste a key? Get one on the site and put it in Plugin key under 1. Connect.", Ui.CARD_WIDTH));
        Ui.link(this, "Get a key on the site", "https://ironcladrs.com/plugin");
    }
}
