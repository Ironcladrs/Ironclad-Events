package com.ironcladrs.events.ui;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import javax.swing.*;

/** Status words remain visible without relying on colour. */
public final class ClaimsList extends JPanel
{
    public ClaimsList(JsonArray claims, long eventId, int cap)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); int count = 0;
        for (JsonElement value : claims)
        {
            if (!value.isJsonObject()) { continue; } JsonObject claim = value.getAsJsonObject();
            if (eventId >= 0 && Json.number(claim, "eventId", -1) != eventId) { continue; } if (++count > cap) { break; }
            String status = Json.text(claim, "status"); if (status.equals("pending")) { status = "waiting"; }
            add(Ui.text((Json.text(claim, "item").isEmpty() ? "" : Json.text(claim, "item") + " · ") + status)); add(Ui.text(Json.text(claim, "label")));
            if (claim.has("killCount") && !claim.get("killCount").isJsonNull()) { add(Ui.text("Drop at KC " + Json.number(claim, "killCount", 0))); }
            if (status.equals("denied")) { add(Ui.text(Json.text(claim, "reason"))); }
        }
        if (count == 0) { add(Ui.text("No recent claims.")); }
    }
}
