package com.ironcladrs.events.ui;

import com.google.gson.*;
import com.ironcladrs.events.IroncladEventsConfig;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.sync.SessionState;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Clan defaults in one press: the clan's loot filters plus every upload the clan recommends, after one
 * confirmation that names each switch and carries RuneLite's third-party warning. Only known upload keys
 * are ever switched on, never anything else the server sends, and nothing is switched off.
 */
public final class ClanSettingsCard extends JPanel
{
    public ClanSettingsCard(IroncladEventsConfig config, SessionState state, IroncladPanel.Actions actions)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setAlignmentX(LEFT_ALIGNMENT);
        setBackground(net.runelite.client.ui.ColorScheme.DARKER_GRAY_COLOR); setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        JsonObject preset = Json.object(state.sync(), "clanPreset");
        if (preset.size() == 0) { preset = Json.object(state.hello(), "clanPreset"); }
        final JsonObject snapshot = preset.deepCopy();
        Ui.heading(this, "Clan defaults");
        if (Json.number(preset, "version", 0) != 1)
        {
            add(Ui.text("Connect to see what the clan recommends. Your own settings are in use.", Ui.CARD_WIDTH));
            if (config.useClanSettings()) { add(Ui.fit(Ui.button("Use personal settings", () -> actions.clanSettings(false)))); }
            return;
        }
        List<Integer> missing = missing(config, snapshot);
        if (config.useClanSettings() && missing.isEmpty())
        {
            JLabel on = Ui.text("On: clan loot filters and every upload the clan recommends.", Ui.CARD_WIDTH); on.setForeground(Ui.GOOD); add(on);
        }
        else
        {
            add(Ui.text(missing.isEmpty() ? "Use the clan's loot filters. Your uploads already match what the clan recommends. You confirm once."
                : "Turn on what the clan recommends in one go: " + missing.size() + " upload" + (missing.size() == 1 ? "" : "s") + " and the clan's loot filters. You confirm once.", Ui.CARD_WIDTH));
            add(Ui.fit(Ui.primary("Use clan defaults", () -> enable(this, config, snapshot, actions))));
        }
        add(Ui.fit(Ui.button("See the clan's settings", () -> JOptionPane.showMessageDialog(this, scroll(preview(snapshot)), "Clan defaults", JOptionPane.INFORMATION_MESSAGE))));
        if (config.useClanSettings()) { add(Ui.fit(Ui.button("Use personal settings", () -> actions.clanSettings(false)))); }
        add(Ui.muted("Personal settings put your own loot filters back. Uploads stay as they are; turn any off in the plugin's settings.", Ui.CARD_WIDTH));
    }
    /** Are the clan defaults offered but not fully on? Drives the button at the top of the panel. */
    public static boolean offered(IroncladEventsConfig config, SessionState state)
    {
        JsonObject preset = preset(state);
        return Json.number(preset, "version", 0) == 1 && (!config.useClanSettings() || !missing(config, preset).isEmpty());
    }
    /** The one confirmation (names each switch, carries the third-party warning), then turns them on. */
    public static void enable(java.awt.Component parent, IroncladEventsConfig config, JsonObject preset, IroncladPanel.Actions actions)
    {
        List<Integer> missing = missing(config, preset);
        if (JOptionPane.showOptionDialog(parent, scroll(confirmText(config, preset)), "Use clan defaults?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
            null, new String[]{"Use clan defaults", "Cancel"}, "Cancel") == JOptionPane.YES_OPTION)
        {
            List<String> keys = new ArrayList<>(); for (int i : missing) { keys.add(IroncladEventsConfig.UPLOAD_KEYS[i]); }
            actions.clanDefaults(keys);
        }
    }
    public static JsonObject preset(SessionState state)
    {
        JsonObject preset = Json.object(state.sync(), "clanPreset");
        return (preset.size() == 0 ? Json.object(state.hello(), "clanPreset") : preset).deepCopy();
    }
    /** Positions in UPLOAD_KEYS the clan recommends that are still off. */
    public static List<Integer> missing(IroncladEventsConfig config, JsonObject preset)
    {
        JsonObject recommended = Json.object(preset, "recommended"); List<Integer> result = new ArrayList<>();
        for (int i = 0; i < IroncladEventsConfig.UPLOAD_KEYS.length; i++)
        { String key = IroncladEventsConfig.UPLOAD_KEYS[i]; if (Json.flag(recommended, key) && !IroncladEventsConfig.upload(config, key)) { result.add(i); } }
        return result;
    }
    public static String confirmText(IroncladEventsConfig config, JsonObject preset)
    {
        StringBuilder text = new StringBuilder(IroncladEventsConfig.THIRD_PARTY_WARNING).append(".\n\nThis turns on:\n");
        for (int i : missing(config, preset)) { text.append("- ").append(IroncladEventsConfig.UPLOAD_LABELS[i]).append('\n'); }
        text.append("- The clan's loot filters (your own are kept for later)\n\nNothing is turned off. Screenshots include your whole game window and chat.")
            .append(" Hover each setting in RuneLite's settings to see exactly what it sends, and turn any off there.");
        return text.toString();
    }
    private static String preview(JsonObject preset)
    {
        JsonObject filters = Json.object(preset, "filters"), recommended = Json.object(preset, "recommended");
        StringBuilder text = new StringBuilder("Clan loot filters\n");
        text.append("Wall of Fame minimum: ").append(Json.number(filters, "minimumWallValue", 0)).append(" gp (0 = the site's minimum)\n");
        text.append("Never send these items: ").append(orNone(Json.text(filters, "ignoredLootItems"))).append('\n');
        text.append("Never send from these: ").append(orNone(Json.text(filters, "ignoredLootSources"))).append("\n\nRecommended uploads\n");
        for (int i = 0; i < IroncladEventsConfig.UPLOAD_KEYS.length; i++)
        { text.append(IroncladEventsConfig.UPLOAD_LABELS[i]).append(": ").append(Json.flag(recommended, IroncladEventsConfig.UPLOAD_KEYS[i]) ? "On" : "Off").append('\n'); }
        text.append("\nRevision ").append(Json.text(preset, "revision")).append(". Filters can stop an event drop from filing.");
        return text.toString();
    }
    private static String orNone(String value) { return value.trim().isEmpty() ? "none" : value; }
    private static JScrollPane scroll(String text)
    {
        JTextArea area = new JTextArea(text, 18, 32); area.setLineWrap(true); area.setWrapStyleWord(true); area.setEditable(false); area.setCaretPosition(0);
        return new JScrollPane(area);
    }
}
