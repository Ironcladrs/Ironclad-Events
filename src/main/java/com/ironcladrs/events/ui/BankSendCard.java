package com.ironcladrs.events.ui;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import java.awt.Component;
import javax.swing.*;
import net.runelite.client.game.ItemManager;

/** Only the exact asked counts are shown. Event-scoped automatic consent starts unchecked. */
public final class BankSendCard extends JPanel
{
    public BankSendCard(JsonObject card, ItemManager items, IroncladPanel.Actions actions)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setAlignmentX(Component.LEFT_ALIGNMENT);
        setBackground(net.runelite.client.ui.ColorScheme.DARKER_GRAY_COLOR); setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        long eventId = Json.number(card, "eventId", -1), precheckId = Json.number(card, "precheckId", -1);
        add(Ui.bold("Bank check", Ui.CARD_WIDTH)); add(Ui.text(Json.text(card, "label"), Ui.CARD_WIDTH));
        Json.object(card, "counts").entrySet().forEach(entry -> {
            String name = Json.text(Json.object(card, "names"), entry.getKey()); if (name.isEmpty()) { name = entry.getKey(); }
            add(Ui.row(name, "x" + entry.getValue().getAsLong(), false));
        });
        add(Ui.muted("Counted from your bank, inventory and equipment only.", Ui.CARD_WIDTH));
        // A drawn switch, not a checkbox: RuneLite's checkbox was hard to see on this card (2026-11-27).
        final boolean[] automatic = {false};
        JLabel state = new JLabel("Send automatically: Off"); state.setFont(net.runelite.client.ui.FontManager.getRunescapeSmallFont()); state.setForeground(Ui.MUTED);
        Ui.Switch toggle = new Ui.Switch(false, on -> { automatic[0] = on; state.setText("Send automatically: " + (on ? "On" : "Off")); state.setForeground(on ? Ui.GOOD : Ui.MUTED); });
        toggle.setToolTipText("For the rest of this event only; forgotten on logout or restart.");
        JPanel row = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 4)); row.setOpaque(false); row.add(toggle); row.add(state);
        add(Ui.fit(row));
        add(Ui.buttons(Ui.primary("Send", () -> actions.bank(eventId, precheckId, automatic[0])), Ui.button("Not now", () -> actions.dismissBank(eventId, precheckId))));
    }
}
