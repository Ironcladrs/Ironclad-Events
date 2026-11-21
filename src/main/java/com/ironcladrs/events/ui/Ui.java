package com.ironcladrs.events.ui;

import java.awt.*;
import java.net.URI;
import javax.swing.*;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.LinkBrowser;

/** Swing components receive text, never server-supplied HTML or arbitrary browser origins. */
public final class Ui
{
    private Ui() {}
    public static String html(String text)
    { return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    public static String plain(String text) { return text == null ? "" : text.replace('<', '‹').replace('>', '›'); }
    public static JLabel name(String text)
    {
        String safe = plain(text); JLabel label = new JLabel(safe.length() > 32 ? safe.substring(0, 31) + "…" : safe);
        label.setToolTipText("<html>" + html(safe) + "</html>"); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
    public static JPanel column()
    {
        JPanel result = new JPanel(); result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
        result.setBackground(ColorScheme.DARK_GRAY_COLOR); result.setAlignmentX(Component.LEFT_ALIGNMENT); return result;
    }
    public static JLabel text(String text)
    {
        String bounded = text == null ? "" : text.substring(0, Math.min(1000, text.length())).replace('\u2019', '\'').replace('\u2018', '\'');
        JLabel label = new JLabel("<html><body style='width:175px'>" + html(bounded) + "</body></html>");
        label.setToolTipText("<html>" + html(bounded) + "</html>"); label.setAlignmentX(Component.LEFT_ALIGNMENT); label.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0)); return label;
    }
    public static void heading(JPanel parent, String label)
    { JLabel title = name(label); title.setFont(title.getFont().deriveFont(Font.BOLD)); parent.add(Box.createVerticalStrut(8)); parent.add(title); }
    public static JButton button(String text, Runnable action)
    { JButton button = new JButton(plain(text)); button.setAlignmentX(Component.LEFT_ALIGNMENT); button.addActionListener(event -> action.run()); return button; }
    public static boolean allowedLink(String url)
    {
        try { URI uri = URI.create(url); return "https".equalsIgnoreCase(uri.getScheme()) && "ironcladrs.com".equalsIgnoreCase(uri.getHost()) && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443); }
        catch (RuntimeException ex) { return false; }
    }
    public static void link(JPanel parent, String label, String url)
    { if (allowedLink(url)) { parent.add(button(label, () -> LinkBrowser.browse(url))); } }
}
