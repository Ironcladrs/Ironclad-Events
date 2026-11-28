package com.ironcladrs.events.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Fits a game item picture into the square sidebar icon: trims its empty border, keeps its shape, centres it. */
public final class SidebarIcon
{
    private SidebarIcon() {}

    public static BufferedImage fit(BufferedImage item, int size)
    {
        if (item == null) { return null; }
        int minX = item.getWidth(), minY = item.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < item.getHeight(); y++)
        {
            for (int x = 0; x < item.getWidth(); x++)
            {
                if ((item.getRGB(x, y) >>> 24) > 16) { minX = Math.min(minX, x); minY = Math.min(minY, y); maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); }
            }
        }
        if (maxX < 0) { return null; }
        int w = maxX - minX + 1, h = maxY - minY + 1;
        double scale = Math.min((double) size / w, (double) size / h);
        int dw = Math.max(1, (int) Math.round(w * scale)), dh = Math.max(1, (int) Math.round(h * scale));
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(item, (size - dw) / 2, (size - dh) / 2, (size - dw) / 2 + dw, (size - dh) / 2 + dh, minX, minY, maxX + 1, maxY + 1, null);
        }
        finally { g.dispose(); }
        return out;
    }
}
