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
    /** The clan's gold (the helm's trim), for the one main action and the team's own row. */
    public static final Color GOLD = new Color(230, 172, 30);
    public static final Color CARD_EDGE = new Color(60, 60, 60);
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
    public static JPanel section(JPanel parent, String key, String title, Set<String> closed) { return section(parent, key, title, null, closed); }
    /** A titled section that folds open and shut; `count` ("2 of 16") sits on the right of the bar. */
    public static JPanel section(JPanel parent, String key, String title, String count, Set<String> closed)
    {
        JPanel body = column(); body.setOpaque(false); body.setBorder(BorderFactory.createEmptyBorder(4, 2, 2, 2));
        JButton header = new JButton(); header.setLayout(new BorderLayout(6, 0));
        JLabel name = new JLabel(plain(title)); name.setFont(FontManager.getRunescapeBoldFont()); name.setForeground(Color.WHITE);
        JLabel right = new JLabel(count == null ? "" : plain(count)); right.setFont(FontManager.getRunescapeSmallFont()); right.setForeground(MUTED);
        header.add(name, BorderLayout.CENTER); header.add(right, BorderLayout.EAST);
        header.setContentAreaFilled(false); header.setOpaque(true); header.setBackground(ColorScheme.DARKER_GRAY_COLOR); header.setFocusPainted(false);
        header.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 8)); header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.getAccessibleContext().setAccessibleName(plain(title));
        Runnable paint = () -> {
            boolean open = !closed.contains(key);
            name.setIcon(new Chevron(open)); header.setToolTipText(open ? "Hide" : "Show"); body.setVisible(open);
        };
        header.addActionListener(event -> {
            if (!closed.remove(key)) { closed.add(key); }
            paint.run(); parent.revalidate(); parent.repaint();
        });
        header.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseEntered(MouseEvent e) { header.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR); }
            @Override public void mouseExited(MouseEvent e) { header.setBackground(ColorScheme.DARKER_GRAY_COLOR); }
        });
        paint.run(); gap(parent, 6); parent.add(fit(header)); parent.add(body); return body;
    }

    // ── Drawn shapes (the RuneScape fonts have no symbols) ───────────────────
    /** A small triangle: pointing down when open, right when shut. */
    public static final class Chevron implements Icon
    {
        private final boolean open;
        public Chevron(boolean open) { this.open = open; }
        @Override public int getIconWidth() { return 10; }
        @Override public int getIconHeight() { return 10; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y)
        {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(MUTED);
            Polygon p = open ? new Polygon(new int[]{x + 1, x + 9, x + 5}, new int[]{y + 3, y + 3, y + 8}, 3) : new Polygon(new int[]{x + 3, x + 8, x + 3}, new int[]{y + 1, y + 5, y + 9}, 3);
            g2.fill(p); g2.dispose();
        }
    }
    /** A task's state: a green tick in a filled circle when done, an empty ring when open. */
    public static final class StateDot implements Icon
    {
        private final boolean done;
        public StateDot(boolean done) { this.done = done; }
        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 12; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y)
        {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (done)
            {
                g2.setColor(GOOD); g2.fillOval(x, y, 11, 11); g2.setColor(ColorScheme.DARKER_GRAY_COLOR); g2.setStroke(new BasicStroke(1.8f));
                g2.drawPolyline(new int[]{x + 3, x + 5, x + 8}, new int[]{y + 6, y + 8, y + 3}, 3);
            }
            else { g2.setColor(ColorScheme.MEDIUM_GRAY_COLOR.brighter()); g2.setStroke(new BasicStroke(1.4f)); g2.drawOval(x + 1, y + 1, 9, 9); }
            g2.dispose();
        }
    }
    /** A thin bar: `fraction` of it filled in `fill`. */
    public static JComponent bar(double fraction, Color fill)
    {
        double f = Double.isNaN(fraction) ? 0 : Math.max(0, Math.min(1, fraction));
        JComponent bar = new JComponent()
        {
            @Override protected void paintComponent(Graphics g)
            {
                g.setColor(ColorScheme.DARK_GRAY_COLOR); g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(fill); g.fillRect(0, 0, (int) Math.round(getWidth() * f), getHeight());
            }
        };
        bar.setPreferredSize(new Dimension(10, 5)); bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5)); bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setToolTipText(Math.round(f * 100) + "%");
        return bar;
    }
    /** "3 of 16" / "3/16" in a score text → 3/16, or NaN. */
    public static double fraction(String text)
    {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d[\\d,]*)\\s*(?:of|/)\\s*(\\d[\\d,]*)").matcher(text == null ? "" : text);
        if (!m.find()) { return Double.NaN; }
        double have = Double.parseDouble(m.group(1).replace(",", "")), need = Double.parseDouble(m.group(2).replace(",", ""));
        return need <= 0 ? Double.NaN : have / need;
    }
    /** A small rounded tag ("Bingo", "Live") in a colour, with its word always written. */
    public static JLabel pill(String text, Color color)
    {
        JLabel label = new JLabel(plain(text))
        {
            @Override protected void paintComponent(Graphics g)
            {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45)); g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(color); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8); g2.dispose(); super.paintComponent(g);
            }
        };
        label.setFont(FontManager.getRunescapeSmallFont()); label.setForeground(color.brighter()); label.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6)); return label;
    }
    /** Tags or small parts side by side on one line. */
    public static JPanel line(JComponent... parts)
    {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0)); row.setOpaque(false);
        for (JComponent part : parts) { if (part != null) { row.add(part); } }
        row.setBorder(BorderFactory.createEmptyBorder(0, -4, 0, 0)); return fit(row);
    }
    /** An on/off switch drawn in RuneLite's colours; the label beside it always says On or Off. */
    public static final class Switch extends JComponent
    {
        private boolean on;
        public Switch(boolean on, java.util.function.Consumer<Boolean> changed)
        {
            this.on = on; setPreferredSize(new Dimension(30, 16)); setMaximumSize(getPreferredSize()); setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setToolTipText(on ? "On" : "Off");
            Runnable flip = () -> { this.on = !this.on; setToolTipText(this.on ? "On" : "Off"); repaint(); changed.accept(this.on); };
            addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { flip.run(); } });
            addKeyListener(new KeyAdapter() { @Override public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) { flip.run(); } } });
            addFocusListener(new FocusAdapter() { @Override public void focusGained(FocusEvent e) { repaint(); } @Override public void focusLost(FocusEvent e) { repaint(); } });
        }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(on ? GOOD.darker() : ColorScheme.MEDIUM_GRAY_COLOR); g2.fillRoundRect(0, 2, 28, 12, 12, 12);
            if (isFocusOwner()) { g2.setColor(Color.WHITE); g2.drawRoundRect(0, 2, 28, 12, 12, 12); }
            g2.setColor(on ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR); g2.fillOval(on ? 15 : 1, 1, 14, 14); g2.dispose();
        }
    }

    // ── Buttons and links ────────────────────────────────────────────────────
    public static JButton button(String text, Runnable action) { return styled(new JButton(plain(text)), action, false); }
    /** The one main action on a screen: the clan's gold, dark text. */
    public static JButton primary(String text, Runnable action) { return styled(new JButton(plain(text)), action, true); }
    /** Flat buttons in RuneLite's own greys, with a hover shade and a visible keyboard focus ring. */
    private static JButton styled(JButton button, Runnable action, boolean main)
    {
        Color base = main ? GOLD : ColorScheme.DARKER_GRAY_COLOR.brighter(), hover = main ? GOLD.brighter() : ColorScheme.DARK_GRAY_HOVER_COLOR;
        button.setAlignmentX(Component.LEFT_ALIGNMENT); button.setFocusPainted(false); button.setContentAreaFilled(false); button.setOpaque(true);
        button.setBackground(base); button.setForeground(main ? new Color(25, 20, 5) : Color.WHITE); button.setFont(FontManager.getRunescapeSmallFont());
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(main ? GOLD.darker() : ColorScheme.MEDIUM_GRAY_COLOR), BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseEntered(MouseEvent e) { if (button.isEnabled()) { button.setBackground(hover); } }
            @Override public void mouseExited(MouseEvent e) { button.setBackground(base); }
        });
        button.addFocusListener(new FocusAdapter()
        {
            @Override public void focusGained(FocusEvent e) { button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE), BorderFactory.createEmptyBorder(5, 8, 5, 8))); }
            @Override public void focusLost(FocusEvent e) { button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(main ? GOLD.darker() : ColorScheme.MEDIUM_GRAY_COLOR), BorderFactory.createEmptyBorder(5, 8, 5, 8))); }
        });
        button.addActionListener(event -> action.run()); return button;
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
