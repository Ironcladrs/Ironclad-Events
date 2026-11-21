package com.ironcladrs.events.ui;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;

/** Capture previews stay local until the member presses Send. */
public final class PrecheckList extends JPanel
{
    public PrecheckList(long eventId, JsonArray prechecks, IroncladPanel.Actions actions, boolean picturesAvailable, boolean bankAvailable)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT);
        int count = 0;
        for (JsonElement value : prechecks)
        {
            if (++count > 100) { break; } if (!value.isJsonObject()) { continue; }
            JsonObject precheck = value.getAsJsonObject(); long id = Json.number(precheck, "id", -1);
            add(Ui.text(Json.text(precheck, "label"))); add(Ui.text(Json.text(precheck, "instructions")));
            String kind = Json.text(precheck, "kind");
            if (kind.equals("bank")) { add(Ui.text(bankAvailable ? "Open your bank to review the asked counts." : "Plugin bank checks are not available yet. Use the site.")); }
            else if (kind.equals("picture"))
            {
                JPanel preview = Ui.column(); add(preview);
                JButton capture = Ui.button("Capture", () -> actions.capture(bytes -> {
                    preview.removeAll();
                    if (bytes == null) { preview.add(Ui.text("Capture unavailable. Check your connection and try again.")); }
                    else
                    {
                        preview.add(new ScreenshotPreview(bytes));
                        JTextField number = new JTextField(), note = new JTextField();
                        number.setMaximumSize(new Dimension(190, 26)); note.setMaximumSize(new Dimension(190, 26));
                        preview.add(Ui.text("Number (optional)")); preview.add(number); preview.add(Ui.text("Note (optional)")); preview.add(note);
                        preview.add(Ui.text("Includes your whole game window and chat. Only you and reviewing staff can see it."));
                        preview.add(Ui.button("Send picture", () -> {
                            actions.picture(eventId, id, number.getText(), note.getText(), bytes);
                            preview.removeAll(); preview.add(Ui.text("Upload requested; see the status below.")); preview.revalidate(); preview.repaint();
                        }));
                        preview.add(Ui.button("Discard", () -> { preview.removeAll(); preview.revalidate(); preview.repaint(); }));
                    }
                    preview.revalidate(); preview.repaint();
                })); capture.setEnabled(picturesAvailable); add(capture);
                if (!picturesAvailable) { add(Ui.text("Plugin picture uploads are not available yet. Use the site.")); }
            }
            else if (kind.equals("collection")) { add(Ui.text("Account sync can provide collection evidence.")); }
            Ui.link(this, "Open on site", Json.text(precheck, "url"));
        }
    }
}
