package com.ironcladrs.events.ui;

import com.ironcladrs.events.IroncladEventsConfig;
import com.ironcladrs.events.sync.AccountLinker;
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
    public SetupCard(IroncladEventsConfig config, SessionState state, AccountLinker.State link, IroncladPanel.Actions actions)
    {
        this.state = state;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel connect = Ui.card(); Ui.gap(this, 6); add(connect);
        connect.add(Ui.bold(state.linked ? "Connection" : "Get connected", Ui.CARD_WIDTH));
        if (!state.message.isEmpty()) { connect.add(Ui.muted(state.message, Ui.CARD_WIDTH)); }
        list = connect;
        row("Connect to ironcladrs.com", config.connect(), null, null); row("Plugin key entered", !config.apiKey().trim().isEmpty(), null, null); row("This account linked", state.linked, null, null);
        Ui.gap(connect, 8); connect.add(new LinkCard(config, link, state.linked, actions)); Ui.gap(connect, 4);

        JPanel uploads = Ui.card(); Ui.gap(this, 6); add(uploads);
        uploads.add(Ui.bold("What this plugin sends", Ui.CARD_WIDTH));
        uploads.add(Ui.muted("Every upload starts off. Once you're linked, press Use clan defaults at the top to turn on what the clan recommends in one go, or turn each one on yourself in the plugin's settings (RuneLite's Configuration page). Nothing is sent until you do.", Ui.CARD_WIDTH));
        Ui.gap(uploads, 4);
        uploads.add(Ui.muted("For new-item updates, turn on collection log notifications in the game's settings.", Ui.CARD_WIDTH));
        Ui.link(uploads, "What is sent", "https://ironcladrs.com/privacy#plugin");
    }
    /** The server feature each switch depends on, so the panel can say when the site has it turned off. */
    static String feature(String key)
    {
        if (key.startsWith("sync")) { return "account"; }
        switch (key)
        {
            case "autoDrops": case "wallOfFameDrops": case "screenshots": return "loot";
            case "liveProgress": return "progress";
            case "autoPbs": case "pbScreenshots": return "pbs";
            case "relayClanChat": return "chat";
            case "autoDeaths": case "deathScreenshots": return "deaths";
            case "sharePets": return "pets";
            case "shareMilestones": return "milestones";
            case "shareClues": return "clues";
            case "shareCombatTasks": case "shareDiaries": return "milestones";
            case "sendPkLoot": return "loot";
            default: return key;
        }
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
