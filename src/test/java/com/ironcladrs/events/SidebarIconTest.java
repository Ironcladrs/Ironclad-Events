package com.ironcladrs.events;

import com.ironcladrs.events.ui.SidebarIcon;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;

public class SidebarIconTest
{
    @Test public void trimsTheEmptyBorderAndKeepsTheShapeInsideTheSquare()
    {
        BufferedImage item = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 4; y < 28; y++) { for (int x = 10; x < 22; x++) { item.setRGB(x, y, 0xFF8B5A2B); } } // 12 wide, 24 tall
        BufferedImage icon = SidebarIcon.fit(item, 16);
        assertEquals(16, icon.getWidth()); assertEquals(16, icon.getHeight());
        assertEquals("fills the height", 0xFF, icon.getRGB(8, 0) >>> 24);
        assertEquals("keeps its width share, so the sides stay clear", 0, icon.getRGB(0, 8) >>> 24);
        assertNull("an empty picture gives no icon", SidebarIcon.fit(new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB), 16));
    }
}
