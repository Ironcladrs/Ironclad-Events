package com.ironcladrs.events.ui;

import com.ironcladrs.events.IroncladEventsConfig;
import com.ironcladrs.events.sync.SessionState;
import java.awt.*;
import javax.swing.*;

/**
 * Consent is edited only through RuneLite configuration, never silently by the setup view.
 * Each upload reads "On" or "Off" in words, with "Not available yet" when the site has it switched off.
 */
public final class SetupCard extends JPanel
{
    private final SessionState state;
    private JPanel list;
    public SetupCard(IroncladEventsConfig config, SessionState state, IroncladPanel.Actions actions)
    {
        this.state = state;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel connect = Ui.card(); Ui.gap(this, 6); add(connect);
        connect.add(Ui.bold(state.linked ? "Connection" : "Get connected", Ui.CARD_WIDTH));
        if (!state.message.isEmpty()) { connect.add(Ui.muted(state.message, Ui.CARD_WIDTH)); }
        list = connect;
        row("Connect to ironcladrs.com", config.connect(), null, null); row("Plugin key entered", !config.apiKey().trim().isEmpty(), null, null); row("This account linked", state.linked, null, null);
        if (!state.linked)
        {
            Ui.gap(connect, 4);
            connect.add(Ui.text("1. Get your key from the site.", Ui.CARD_WIDTH));
            connect.add(Ui.text("2. Paste it in this plugin's settings and turn on Connect.", Ui.CARD_WIDTH));
            connect.add(Ui.text("3. Press Sync now.", Ui.CARD_WIDTH));
        }
        Ui.gap(connect, 4); Ui.link(connect, "Get my key", "https://ironcladrs.com/settings#plugin");
        connect.add(Ui.fit(Ui.button("Where are the settings?", () -> JOptionPane.showMessageDialog(this,
            "Open RuneLite's Configuration panel, search for Ironclad Events, then click its settings cog.", "Ironclad Events settings", JOptionPane.INFORMATION_MESSAGE))));

        Ui.gap(this, 6); add(new ClanSettingsCard(config, state, actions));

        JPanel uploads = Ui.card(); Ui.gap(this, 6); add(uploads); list = uploads;
        uploads.add(Ui.bold("What this plugin sends", Ui.CARD_WIDTH));
        uploads.add(Ui.muted("Everything starts off. Turn each one on in the plugin's settings.", Ui.CARD_WIDTH)); Ui.gap(uploads, 4);
        row("Event drops", config.autoDrops(), "loot", "Items, source, kill count and time.");
        row("Screenshots (whole window and chat)", config.screenshots(), null, "The whole game window, including chat.");
        row("Asked bank counts", config.bankPrechecks(), "bankPrechecks", "Only the counts a precheck asks for, shown to you first.");
        row("Live XP, KC and event time", config.liveProgress(), "progress", "XP, kill counts and minutes active during an event.");
        row("Account: log, CAs, quests, levels", config.syncAccount(), "account", "Collection log, combat achievements, quests, diaries, levels and account type.");
        row("New personal bests", config.autoPbs(), "pbs", "Time, worn gear and combat levels.");
        row("Clan chat", config.relayClanChat(), "chat", "Sender, message text and time.");
        row("Your deaths", config.autoDeaths(), "deaths", "An announcement only.");
        row("Death screenshots to clan Discord", config.deathScreenshots(), null, "Published to the clan Discord.");
        row("New pets", config.sharePets(), "pets", null);
        row("Level milestones", config.shareMilestones(), "milestones", null);
        row("Clue counts", config.shareClues(), "clues", null);
        Ui.gap(uploads, 4);
        uploads.add(Ui.muted("For new-item updates, turn on collection log notifications in the game's settings.", Ui.CARD_WIDTH));
        Ui.link(uploads, "What is sent", "https://ironcladrs.com/privacy#plugin");
    }
    private void row(String label, boolean on, String feature, String detail)
    {
        boolean unavailable = feature != null && state.linked && !state.feature(feature);
        JPanel row = new JPanel(new BorderLayout(6, 0)); row.setOpaque(false); row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        JLabel name = Ui.text(label, 145); if (detail != null) { name.setToolTipText(Ui.plain(detail)); } row.add(name, BorderLayout.CENTER);
        JLabel value = new JLabel(on ? "On" : "Off"); value.setForeground(on ? Ui.GOOD : Ui.MUTED); value.setVerticalAlignment(SwingConstants.TOP);
        row.add(value, BorderLayout.EAST); list.add(Ui.fit(row));
        if (unavailable) { list.add(Ui.muted("Not available on the site yet", Ui.CARD_WIDTH)); }
    }
}
