package com.ironcladrs.events;

import com.ironcladrs.events.ui.ScreenshotPreview;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScreenshotPreviewTest
{
    @Test public void enlargedViewKeepsOriginalPixelsInsideABoundedScrollableWindow() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ImageIcon image = new ImageIcon(new BufferedImage(1600, 1000, BufferedImage.TYPE_INT_RGB));
            JScrollPane full = ScreenshotPreview.fullSize(image);
            assertSame(image, ((JLabel) full.getViewport().getView()).getIcon());
            assertEquals(new Dimension(900, 650), full.getPreferredSize());
            full.setSize(full.getPreferredSize()); full.doLayout(); full.getViewport().doLayout();
            assertTrue(full.getViewport().getViewSize().width > full.getViewport().getExtentSize().width);
            assertTrue(full.getViewport().getViewSize().height > full.getViewport().getExtentSize().height);
        });
    }
}
