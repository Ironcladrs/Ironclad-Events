package com.ironcladrs.events.ui;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import java.awt.*;
import javax.swing.*;

/** Each claim leads with its status word; the colour only repeats what the word says. */
public final class ClaimsList extends JPanel
{
    public ClaimsList(JsonArray claims, long eventId, int cap)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT); int count = 0;
        for (JsonElement value : claims)
        {
            if (!value.isJsonObject()) { continue; } JsonObject claim = value.getAsJsonObject();
            if (eventId >= 0 && Json.number(claim, "eventId", -1) != eventId) { continue; } if (++count > cap) { break; }
            String status = Json.text(claim, "status"), item = Json.text(claim, "item"), label = Json.text(claim, "label");
            JPanel row = new JPanel(new BorderLayout(6, 0)); row.setOpaque(false); row.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            JLabel what = Ui.name(item.isEmpty() ? label : item); what.setForeground(Color.WHITE);
            row.add(what, BorderLayout.CENTER); row.add(Ui.status(status), BorderLayout.EAST); add(Ui.fit(row));
            if (!item.isEmpty() && !label.isEmpty()) { add(Ui.muted(label, Ui.WIDTH)); }
            if (claim.has("killCount") && !claim.get("killCount").isJsonNull()) { add(Ui.muted("Drop at KC " + Json.number(claim, "killCount", 0), Ui.WIDTH)); }
            if (status.equals("denied") && !Json.text(claim, "reason").isEmpty()) { add(Ui.muted("Reason: " + Json.text(claim, "reason"), Ui.WIDTH)); }
        }
        if (count == 0) { add(Ui.muted("No claims yet. Drops you file show up here with their status.", Ui.WIDTH)); }
    }
}
