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
        JCheckBox automatic = new JCheckBox("Send automatically for this event"); automatic.setOpaque(false); automatic.setToolTipText("For the rest of this event only; forgotten on logout or restart."); add(automatic);
        add(Ui.buttons(Ui.button("Send", () -> actions.bank(eventId, precheckId, automatic.isSelected())), Ui.button("Not now", () -> actions.dismissBank(eventId, precheckId))));
    }
}
