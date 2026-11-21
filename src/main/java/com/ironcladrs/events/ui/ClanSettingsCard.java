package com.ironcladrs.events.ui;

import com.google.gson.*;
import com.ironcladrs.events.IroncladEventsConfig;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.sync.SessionState;
import javax.swing.*;

/** Following clan filters never changes credentials, upload consent or personal overrides. */
public final class ClanSettingsCard extends JPanel
{
    public ClanSettingsCard(IroncladEventsConfig config, SessionState state, IroncladPanel.Actions actions)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setAlignmentX(LEFT_ALIGNMENT);
        setBackground(net.runelite.client.ui.ColorScheme.DARKER_GRAY_COLOR); setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        JsonObject preset = Json.object(state.sync(), "clanPreset");
        if (preset.size() == 0) { preset = Json.object(state.hello(), "clanPreset"); }
        final JsonObject snapshot = preset.deepCopy();
        Ui.heading(this, "Clan settings");
        if (Json.number(preset, "version", 0) != 1)
        {
            add(Ui.text("Connect to preview the clan's current defaults. Personal filters are in use.", Ui.CARD_WIDTH));
            if (config.useClanSettings()) { add(Ui.fit(Ui.button("Use personal settings", () -> actions.clanSettings(false)))); }
            return;
        }
        add(Ui.text(config.useClanSettings() ? "Following clan loot filters · " + Json.text(preset, "revision") : "Using personal loot filters", Ui.CARD_WIDTH));
        add(Ui.fit(Ui.button("Preview clan settings", () -> show(snapshot))));
        add(Ui.fit(Ui.button(config.useClanSettings() ? "Use personal settings" : "Use clan settings", () -> {
            if (config.useClanSettings()) { actions.clanSettings(false); return; }
            JTextArea text = preview(snapshot);
            if (JOptionPane.showConfirmDialog(this, new JScrollPane(text), "Use clan settings?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE) == JOptionPane.OK_OPTION)
            { actions.clanSettings(true); }
        })));
        add(Ui.muted("Clan changes update filters while this is on. Upload switches remain yours; enable them in RuneLite settings.", Ui.CARD_WIDTH));
    }
    private void show(JsonObject preset)
    { JOptionPane.showMessageDialog(this, new JScrollPane(preview(preset)), "Clan settings preview", JOptionPane.INFORMATION_MESSAGE); }
    private static JTextArea preview(JsonObject preset)
    {
        JsonObject filters = Json.object(preset, "filters"), recommended = Json.object(preset, "recommended");
        StringBuilder text = new StringBuilder("Follow the clan's filters until you switch back. Your personal filters are preserved.\n\n");
        text.append("Non-event Wall of Fame minimum: ").append(Json.number(filters, "minimumWallValue", 0)).append(" GP (0 = server minimum).\n");
        text.append("Ignore drops containing only: ").append(Json.text(filters, "ignoredLootItems")).append("\nIgnore sources: ").append(Json.text(filters, "ignoredLootSources")).append("\nThese exclusions can suppress event submissions.\n\nUpload recommendations (not applied):\n");
        for (String[] row : new String[][]{{"autoDrops","Event drops"},{"screenshots","Drop/PB screenshots"},{"bankPrechecks","Asked bank counts"},{"liveProgress","Live event progress"},{"syncAccount","Account snapshots"},{"autoPbs","Personal bests"},{"autoDeaths","Own deaths"},{"deathScreenshots","Public death screenshots"},{"sharePets","Pets"},{"shareMilestones","Levels"},{"shareClues","Clues"},{"relayClanChat","Clan chat"}})
        { text.append(row[1]).append(": ").append(Json.flag(recommended, row[0]) ? "On" : "Off").append('\n'); }
        text.append("\nEnable each upload yourself in RuneLite. Screenshots include chat; death pictures are published to the clan Discord. Server availability still applies.");
        JTextArea area = new JTextArea(text.toString(), 18, 32); area.setLineWrap(true); area.setWrapStyleWord(true); area.setEditable(false); area.setCaretPosition(0); return area;
    }
}
