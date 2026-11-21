package com.ironcladrs.events.ui;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import net.runelite.client.game.ItemManager;

/** Build capped rows once per sync. Searching only toggles existing Swing rows locally. */
public final class TaskList extends JPanel
{
    private final List<JPanel> rows = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    public TaskList(JsonArray tasks, ItemManager items)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField search = new JTextField(); search.setMaximumSize(new Dimension(195, 26)); search.setToolTipText("Search tasks locally"); add(search);
        String category = null; int count = 0;
        for (JsonElement value : tasks)
        {
            if (++count > 300) { break; } if (!value.isJsonObject()) { continue; }
            JsonObject task = value.getAsJsonObject(); String nextCategory = Json.text(task, "category");
            JPanel row = Ui.column();
            if (!nextCategory.equals(category)) { category = nextCategory; row.add(Ui.text(category)); }
            String title = Json.text(task, "label");
            JsonArray ids = Json.array(task, "itemIds");
            if (ids.size() > 0 && ids.get(0).isJsonPrimitive())
            { try { int id = ids.get(0).getAsInt(); if (id > 0 && id < 100000) { row.add(new JLabel(new ImageIcon(items.getImage(id)))); } } catch (RuntimeException ignored) { /* Optional sprite. */ } }
            row.add(Ui.name((Json.flag(task, "done") ? "Done: " : "") + title)); row.add(Ui.text(Json.text(task, "progressText")));
            Ui.link(row, "Open task", Json.text(task, "url"));
            rows.add(row); labels.add(Json.norm(title + " " + category)); add(row);
        }
        search.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter()
            { String query = Json.norm(search.getText()); for (int i = 0; i < rows.size(); i++) { rows.get(i).setVisible(labels.get(i).contains(query)); } revalidate(); repaint(); }
        });
    }
}
