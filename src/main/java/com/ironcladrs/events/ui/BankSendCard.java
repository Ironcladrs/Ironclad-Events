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
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT);
        long eventId = Json.number(card, "eventId", -1), precheckId = Json.number(card, "precheckId", -1);
        add(Ui.text(Json.text(card, "label")));
        Json.object(card, "counts").entrySet().forEach(entry -> {
            String name = Json.text(Json.object(card, "names"), entry.getKey()); if (name.isEmpty()) { name = entry.getKey(); }
            add(Ui.text(name + " ×" + entry.getValue().getAsLong()));
        });
        add(Ui.text("Sources: bank, inventory, equipment. Other storage is not checked."));
        JCheckBox automatic = new JCheckBox("Send automatically for this event"); automatic.setOpaque(false); automatic.setToolTipText("For the rest of this event only; forgotten on logout or restart."); add(automatic);
        JButton send = Ui.button("Send", () -> actions.bank(eventId, precheckId, automatic.isSelected())); add(send);
        add(Ui.button("Not now", () -> actions.dismissBank(eventId, precheckId)));
    }
}
