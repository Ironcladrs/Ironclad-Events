package com.ironcladrs.events.ui;

import java.awt.*;
import javax.swing.*;

/** Preview the captured bytes locally; viewing never sends or captures anything. */
public final class ScreenshotPreview extends JPanel
{
    public ScreenshotPreview(byte[] bytes)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false);
        ImageIcon original = new ImageIcon(bytes);
        if (original.getIconWidth() <= 0 || original.getIconHeight() <= 0) { add(Ui.text("Image preview unavailable.")); return; }
        add(new JLabel(new ImageIcon(original.getImage().getScaledInstance(180, -1, Image.SCALE_SMOOTH))));
        add(Ui.button("View full size", () -> JOptionPane.showMessageDialog(this, fullSize(original), "Captured screenshot — not yet sent", JOptionPane.PLAIN_MESSAGE)));
    }
    public static JScrollPane fullSize(ImageIcon original)
    {
        JScrollPane scroll = new JScrollPane(new JLabel(original));
        scroll.setPreferredSize(new Dimension(Math.min(900, original.getIconWidth() + 24), Math.min(650, original.getIconHeight() + 24)));
        scroll.getVerticalScrollBar().setUnitIncrement(24); scroll.getHorizontalScrollBar().setUnitIncrement(24);
        return scroll;
    }
}
