package com.ironcladrs.events.ui;

import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import javax.swing.*;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

/**
 * Shared sidebar building blocks. Swing components receive text, never server-supplied HTML or
 * arbitrary browser origins. Glyphs stay inside Latin-1 because the RuneScape fonts lack most symbols,
 * and every colour is paired with a word so status never depends on colour alone.
 */
public final class Ui
{
    private Ui() {}

    // ── Colours ──────────────────────────────────────────────────────────────
    public static final Color TEXT = new Color(220, 220, 220);
    public static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;
    public static final Color GOOD = ColorScheme.PROGRESS_COMPLETE_COLOR;
    public static final Color BAD = ColorScheme.PROGRESS_ERROR_COLOR;
    public static final Color ACCENT = ColorScheme.BRAND_ORANGE;
    /**
     * Wrapped-text widths in real pixels: the sidebar column, a padded card, and beside a 36 px item picture.
     * Swing's HTML renderer scales CSS px by 1.3, so {@link #css} converts before writing the style.
     */
    public static final int WIDTH = 200, CARD_WIDTH = 185, BESIDE_ICON = 155;
    public static int css(int pixels) { return pixels * 10 / 13; }

    // ── Text ─────────────────────────────────────────────────────────────────
    public static String html(String text)
    { return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    public static String plain(String text) { return text == null ? "" : text.replace('<', '‹').replace('>', '›'); }
    private static String bounded(String text)
    { return text == null ? "" : text.substring(0, Math.min(1000, text.length())).replace('’', '\'').replace('‘', '\''); }

    public static JLabel name(String text)
    {
        String safe = plain(text); JLabel label = new JLabel(safe.length() > 32 ? safe.substring(0, 31) + "…" : safe);
        label.setToolTipText("<html>" + html(safe) + "</html>"); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
    public static JLabel text(String text) { return text(text, WIDTH); }
    public static JLabel text(String text, int width)
    {
        String value = bounded(text);
        JLabel label = new JLabel("<html><body style='width:" + css(width) + "px'>" + html(value) + "</body></html>");
        label.setToolTipText(value.isEmpty() ? null : "<html>" + html(value) + "</html>");
        label.setAlignmentX(Component.LEFT_ALIGNMENT); label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0)); return label;
    }
    public static JLabel muted(String text, int width) { JLabel label = text(text, width); label.setForeground(MUTED); return label; }
    public static JLabel bold(String text, int width)
    { JLabel label = text(text, width); label.setFont(FontManager.getRunescapeBoldFont()); label.setForeground(Color.WHITE); return label; }

    // ── Layout ───────────────────────────────────────────────────────────────
    public static JPanel column()
    {
        JPanel result = new JPanel(); result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
        result.setBackground(ColorScheme.DARK_GRAY_COLOR); result.setAlignmentX(Component.LEFT_ALIGNMENT); return result;
    }
    /** One box level: a darker panel for a thing read as a unit (the event, the team, a precheck). */
    public static JPanel card()
    {
        JPanel card = column(); card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        card.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8)); return card;
    }
    public static void gap(JPanel parent, int height) { parent.add(Box.createVerticalStrut(height)); }
    /** Stops BoxLayout from stretching a row taller than its content. */
    public static <T extends JComponent> T fit(T component)
    {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height)); return component;
    }
    /** Name on the left, value on the right, on one line. */
    public static JPanel row(String left, String right, boolean strong)
    {
        JPanel row = new JPanel(new BorderLayout(6, 0)); row.setOpaque(false);
        JLabel name = name(left), value = new JLabel(plain(right));
        name.setForeground(strong ? Color.WHITE : TEXT); value.setForeground(strong ? Color.WHITE : MUTED);
        if (strong) { name.setFont(FontManager.getRunescapeBoldFont()); value.setFont(FontManager.getRunescapeBoldFont()); }
        row.add(name, BorderLayout.CENTER); row.add(value, BorderLayout.EAST);
        row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0)); return fit(row);
    }
    /** Buttons side by side, sharing the width. */
    public static JPanel buttons(JButton... buttons)
    {
        JPanel row = new JPanel(new GridLayout(1, buttons.length, 4, 0)); row.setOpaque(false);
        for (JButton button : buttons) { row.add(button); }
        return fit(row);
    }

    // ── Headings and sections ────────────────────────────────────────────────
    public static void heading(JPanel parent, String label)
    {
        JLabel title = name(label); title.setFont(FontManager.getRunescapeBoldFont()); title.setForeground(Color.WHITE);
        gap(parent, 8); parent.add(title); gap(parent, 2);
    }
    /**
     * A titled section that folds open and shut. Folding only hides the body, so it never rebuilds lists;
     * the closed set lives on the panel, so a section stays as the member left it after each sync.
     */
    public static JPanel section(JPanel parent, String key, String title, Set<String> closed)
    {
        JPanel body = column(); body.setOpaque(false);
        JButton header = new JButton();
        header.setFont(FontManager.getRunescapeBoldFont()); header.setForeground(Color.WHITE);
        header.setHorizontalAlignment(SwingConstants.LEFT); header.setBorderPainted(false); header.setContentAreaFilled(false); header.setFocusPainted(true);
        header.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0)); header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Runnable paint = () -> {
            boolean open = !closed.contains(key);
            header.setText((open ? "- " : "+ ") + plain(title)); header.setToolTipText(open ? "Hide" : "Show"); body.setVisible(open);
        };
        header.addActionListener(event -> {
            if (!closed.remove(key)) { closed.add(key); }
            paint.run(); parent.revalidate(); parent.repaint();
        });
        paint.run(); gap(parent, 6); parent.add(fit(header)); parent.add(body); return body;
    }

    // ── Buttons and links ────────────────────────────────────────────────────
    public static JButton button(String text, Runnable action)
    {
        JButton button = new JButton(plain(text)); button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setFocusPainted(false); button.addActionListener(event -> action.run()); return button;
    }
    public static boolean allowedLink(String url)
    {
        try { URI uri = URI.create(url); return "https".equalsIgnoreCase(uri.getScheme()) && "ironcladrs.com".equalsIgnoreCase(uri.getHost()) && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443); }
        catch (RuntimeException ex) { return false; }
    }
    public static void link(JPanel parent, String label, String url)
    { if (allowedLink(url)) { parent.add(fit(button(label, () -> LinkBrowser.browse(url)))); } }
    /** Makes a label open a clan link when clicked or activated from the keyboard. */
    public static JLabel linked(JLabel label, String url)
    {
        if (!allowedLink(url)) { return label; }
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); label.setFocusable(true);
        label.setToolTipText("Open on ironcladrs.com");
        label.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { LinkBrowser.browse(url); } });
        label.addKeyListener(new KeyAdapter()
        { @Override public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) { LinkBrowser.browse(url); } } });
        return label;
    }

    // ── Status words and times ───────────────────────────────────────────────
    /** A claim or precheck status as a word in its colour. */
    public static JLabel status(String status)
    {
        String word = status == null ? "" : status.equals("pending") ? "waiting" : status;
        JLabel label = new JLabel(word.isEmpty() ? "" : Character.toUpperCase(word.charAt(0)) + word.substring(1));
        label.setFont(FontManager.getRunescapeBoldFont());
        label.setForeground(word.equals("approved") ? GOOD : word.equals("denied") ? BAD : ACCENT); return label;
    }
    /** "2d 4h", "3h 12m" or "12m". */
    public static String span(long minutes)
    {
        long m = Math.max(0, minutes), days = m / 1440, hours = (m % 1440) / 60;
        return days > 0 ? days + "d " + hours + "h" : hours > 0 ? hours + "h " + m % 60 + "m" : m % 60 + "m";
    }
    /** "just now", "4 min ago", "2 h ago". */
    public static String ago(Instant then, Instant now)
    {
        long seconds = Math.max(0, Duration.between(then, now).getSeconds());
        return seconds < 60 ? "just now" : seconds < 3600 ? seconds / 60 + " min ago" : seconds / 3600 + " h ago";
    }
}
