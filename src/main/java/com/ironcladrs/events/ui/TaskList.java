package com.ironcladrs.events.ui;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import net.runelite.client.game.ItemManager;

/**
 * Build capped rows once per sync, grouped so each category has one heading. Searching only toggles
 * existing Swing rows locally.
 * Each row is the item picture beside the task; clicking the name opens the task, so a long list
 * carries no stack of identical buttons. Finished tasks say "Done" as well as turning grey.
 */
public final class TaskList extends JPanel
{
    private final List<JComponent> rows = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    public TaskList(JsonArray tasks, ItemManager items)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField search = new JTextField(); search.setToolTipText("Search tasks"); search.getAccessibleContext().setAccessibleName("Search tasks");
        add(Ui.muted("Search tasks", Ui.WIDTH)); add(Ui.fit(search)); Ui.gap(this, 4);
        String category = null; int count = 0;
        for (JsonObject task : byCategory(tasks))
        {
            if (++count > 300) { break; }
            String nextCategory = Json.text(task, "category");
            if (!nextCategory.equals(category))
            {
                category = nextCategory;
                if (!category.isEmpty()) { JLabel title = Ui.text(category); title.setForeground(Ui.ACCENT); title.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0)); add(title); }
            }
            String title = Json.text(task, "label"); boolean done = Json.flag(task, "done");
            JPanel row = new JPanel(new BorderLayout(6, 0)); row.setOpaque(false); row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            JLabel picture = icon(task, items); if (picture != null) { row.add(picture, BorderLayout.WEST); }
            JPanel words = Ui.column(); words.setOpaque(false);
            JLabel name = Ui.linked(Ui.text((done ? "Done: " : "") + title, picture == null ? Ui.WIDTH : Ui.BESIDE_ICON), Json.text(task, "url"));
            name.setForeground(done ? Ui.MUTED : Color.WHITE); words.add(name);
            String progress = Json.text(task, "progressText"); if (!progress.isEmpty()) { words.add(Ui.muted(progress, picture == null ? Ui.WIDTH : Ui.BESIDE_ICON)); }
            row.add(words, BorderLayout.CENTER); Ui.fit(row);
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
    /** One heading per category, in the order each category first appears; tasks keep their order inside it. */
    public static List<JsonObject> byCategory(JsonArray tasks)
    {
        Map<String, List<JsonObject>> groups = new LinkedHashMap<>();
        for (JsonElement value : tasks)
        { if (value.isJsonObject()) { JsonObject task = value.getAsJsonObject(); groups.computeIfAbsent(Json.text(task, "category"), key -> new ArrayList<>()).add(task); } }
        List<JsonObject> result = new ArrayList<>(); groups.values().forEach(result::addAll); return result;
    }
    private static JLabel icon(JsonObject task, ItemManager items)
    {
        JsonArray ids = Json.array(task, "itemIds");
        if (items == null || ids.size() == 0 || !ids.get(0).isJsonPrimitive()) { return null; }
        try
        {
            int id = ids.get(0).getAsInt(); if (id <= 0 || id >= 100000) { return null; }
            JLabel label = new JLabel(); label.setPreferredSize(new Dimension(36, 32)); label.setVerticalAlignment(SwingConstants.TOP);
            items.getImage(id).addTo(label); return label;
        }
        catch (RuntimeException ignored) { return null; /* Optional sprite. */ }
    }
}
