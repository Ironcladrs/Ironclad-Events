package com.ironcladrs.events.ui;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
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
    private final List<Boolean> finished = new ArrayList<>();
    private final List<JComponent> headings = new ArrayList<>();
    private final List<Integer> headingOf = new ArrayList<>();
    private String show = "open";
    private String query = "";
    public TaskList(JsonArray tasks, ItemManager items)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT);
        // Open / Done / All, then a search box: the list starts on what is left to do.
        JToggleButton open = chip("Open"), done = chip("Done"), all = chip("All");
        ButtonGroup group = new ButtonGroup(); group.add(open); group.add(done); group.add(all); open.setSelected(true);
        JPanel chips = new JPanel(new GridLayout(1, 3, 4, 0)); chips.setOpaque(false); chips.add(open); chips.add(done); chips.add(all); add(Ui.fit(chips));
        open.addActionListener(e -> { show = "open"; filter(); }); done.addActionListener(e -> { show = "done"; filter(); }); all.addActionListener(e -> { show = "all"; filter(); });
        Ui.gap(this, 4);
        JTextField search = new JTextField(); search.setToolTipText("Search by task, boss, item or activity");
        // Hint inside the empty box (RuneLite's look is FlatLaf, which draws it).
        search.putClientProperty("JTextField.placeholderText", "Boss, item or activity"); search.getAccessibleContext().setAccessibleName("Search tasks");
        search.setBackground(ColorScheme.DARKER_GRAY_COLOR); search.setForeground(Color.WHITE); search.setCaretColor(Color.WHITE);
        search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), BorderFactory.createEmptyBorder(3, 5, 3, 5)));
        JPanel find = new JPanel(new BorderLayout(6, 0)); find.setOpaque(false);
        JLabel label = new JLabel("Search"); label.setFont(FontManager.getRunescapeSmallFont()); label.setForeground(Ui.MUTED); label.setLabelFor(search);
        find.add(label, BorderLayout.WEST); find.add(search, BorderLayout.CENTER); add(Ui.fit(find)); Ui.gap(this, 2);
        String category = null; int count = 0;
        for (JsonObject task : byCategory(tasks))
        {
            if (++count > 300) { break; }
            String nextCategory = heading(task);
            if (!nextCategory.equals(category))
            {
                category = nextCategory;
                if (!category.isEmpty())
                {
                    JLabel title = new JLabel(Ui.plain(category)); title.setFont(FontManager.getRunescapeSmallFont()); title.setForeground(Ui.GOLD);
                    title.setBorder(BorderFactory.createEmptyBorder(8, 0, 2, 0)); title.setAlignmentX(Component.LEFT_ALIGNMENT); add(title); headings.add(title);
                }
            }
            String title = Json.text(task, "label"); boolean isDone = Json.flag(task, "done");
            JPanel row = new JPanel(new BorderLayout(6, 0)); row.setOpaque(false); row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            JLabel picture = icon(task, items);
            JLabel state = new JLabel(new Ui.StateDot(isDone)); state.setVerticalAlignment(SwingConstants.TOP); state.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0)); left.setOpaque(false); left.add(state); if (picture != null) { left.add(picture); }
            row.add(left, BorderLayout.WEST);
            int width = picture == null ? 140 : 105;
            JLabel name = Ui.linked(Ui.text(title, width), Json.text(task, "url")); name.setForeground(isDone ? Ui.MUTED : Color.WHITE);
            row.add(name, BorderLayout.CENTER);
            String progress = Json.text(task, "progressText"), shortText = shortProgress(progress);
            if (!shortText.isEmpty())
            {
                JLabel value = new JLabel(shortText); value.setFont(FontManager.getRunescapeSmallFont()); value.setForeground(isDone ? Ui.GOOD : Ui.MUTED);
                value.setVerticalAlignment(SwingConstants.TOP); value.setToolTipText(Ui.plain(progress)); row.add(value, BorderLayout.EAST);
            }
            Ui.fit(row);
            JComponent wrapped = row;
            if (shortText.isEmpty() && !progress.isEmpty())
            { JPanel stack = Ui.column(); stack.setOpaque(false); stack.add(row); JLabel more = Ui.muted(progress, Ui.WIDTH - 20); more.setBorder(BorderFactory.createEmptyBorder(0, 20, 2, 0)); stack.add(more); wrapped = Ui.fit(stack); }
            // What counts for it, in the staff's own words (2026-11-27: "show what is accepted").
            String accepts = Json.text(task, "accepts");
            if (!accepts.isEmpty())
            {
                JPanel stack = Ui.column(); stack.setOpaque(false); stack.add(wrapped);
                JLabel counts = Ui.muted("Counts: " + accepts, Ui.WIDTH - 20); counts.setBorder(BorderFactory.createEmptyBorder(0, 20, 2, 0)); stack.add(counts);
                wrapped = Ui.fit(stack);
            }
            rows.add(wrapped); labels.add(Json.norm(title + " " + category + " " + Json.text(task, "category") + " " + Json.text(task, "search"))); finished.add(isDone); headingOf.add(headings.size() - 1); add(wrapped);
        }
        search.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent e) { query = Json.norm(search.getText()); filter(); }
            @Override public void removeUpdate(DocumentEvent e) { query = Json.norm(search.getText()); filter(); }
            @Override public void changedUpdate(DocumentEvent e) { query = Json.norm(search.getText()); filter(); }
        });
        filter();
    }
    /** "your team 3 of 10" → "3/10"; anything else stays as a line under the task. */
    static String shortProgress(String text)
    {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(?:your team |you )?([\\d,]+) of ([\\d,]+)$").matcher(text == null ? "" : text.trim());
        return m.find() ? m.group(1) + "/" + m.group(2) : "";
    }
    private void filter()
    {
        boolean[] headingUsed = new boolean[headings.size()];
        for (int i = 0; i < rows.size(); i++)
        {
            boolean visible = labels.get(i).contains(query) && (show.equals("all") || show.equals("done") == finished.get(i));
            rows.get(i).setVisible(visible); if (visible && headingOf.get(i) >= 0) { headingUsed[headingOf.get(i)] = true; }
        }
        for (int h = 0; h < headings.size(); h++) { headings.get(h).setVisible(headingUsed[h]); }
        revalidate(); repaint();
    }
    private static JToggleButton chip(String text)
    {
        JToggleButton chip = new JToggleButton(text); chip.setFocusPainted(false); chip.setContentAreaFilled(false); chip.setOpaque(true);
        chip.setFont(FontManager.getRunescapeSmallFont()); chip.setForeground(Color.WHITE); chip.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // A gold underline and bold text mark the chosen filter: RuneLite's look paints over a toggle's
        // background, so a background shade alone didn't show (2026-11-27), as on the Event / My claims tabs.
        Runnable paint = () -> {
            boolean on = chip.isSelected();
            chip.setBackground(on ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR); chip.setForeground(on ? Color.WHITE : Ui.MUTED);
            chip.setFont(on ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeSmallFont());
            chip.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, on ? Ui.GOLD : ColorScheme.DARKER_GRAY_COLOR), BorderFactory.createEmptyBorder(4, 4, 2, 4)));
            chip.getAccessibleContext().setAccessibleName(chip.getText() + (on ? ", chosen" : ""));
        };
        chip.addItemListener(e -> paint.run()); paint.run(); return chip;
    }
    /** One heading per category, in the order each category first appears; tasks keep their order inside it. */
    /** The site's own grouping (board, region, rope, column, level, floor), or the category before it sent one. */
    public static String heading(JsonObject task) { String group = Json.text(task, "group"); return group.isEmpty() ? Json.text(task, "category") : group; }
    public static List<JsonObject> byCategory(JsonArray tasks)
    {
        Map<String, List<JsonObject>> groups = new LinkedHashMap<>();
        for (JsonElement value : tasks)
        { if (value.isJsonObject()) { JsonObject task = value.getAsJsonObject(); groups.computeIfAbsent(heading(task), key -> new ArrayList<>()).add(task); } }
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
