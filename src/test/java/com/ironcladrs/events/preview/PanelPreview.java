/*
 * BSD 2-Clause License. See LICENSE.
 */
package com.ironcladrs.events.preview;

import com.google.gson.*;
import com.ironcladrs.events.IroncladEventsConfig;
import com.ironcladrs.events.ui.IroncladPanel;
import com.ironcladrs.events.sync.SessionState;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Development only (never packaged): draws the side panel offscreen from a saved sync payload so a
 * layout change can be looked at without starting the game client. Usage: PanelPreview sync.json out.png [view]
 */
public final class PanelPreview
{
	private static void layout(Component c)
	{
		c.doLayout();
		if (c instanceof Container) { for (Component child : ((Container) c).getComponents()) { layout(child); } }
	}

	public static void main(String[] args) throws Exception
	{
		JsonObject sync = new JsonParser().parse(new FileReader(args[0])).getAsJsonObject();
		IroncladEventsConfig config = (IroncladEventsConfig) Proxy.newProxyInstance(IroncladEventsConfig.class.getClassLoader(),
			new Class<?>[]{IroncladEventsConfig.class}, (proxy, method, a) -> method.isDefault()
				? java.lang.invoke.MethodHandles.privateLookupIn(IroncladEventsConfig.class, java.lang.invoke.MethodHandles.lookup())
				.unreflectSpecial(method, IroncladEventsConfig.class).bindTo(proxy).invokeWithArguments(a == null ? new Object[0] : a)
				: null);
		IroncladPanel.Actions actions = new IroncladPanel.Actions()
		{
			public void sync() {}
			public void bank(long e, long p, boolean auto) {}
			public void dismissBank(long e, long p) {}
			public void capture(Consumer<byte[]> preview) {}
			public void picture(long e, long p, String v, String n, byte[] b) {}
			public void respond(long id, String r) {}
		};
		JsonObject hello = new JsonObject();
		JsonObject features = new JsonObject();
		for (String f : new String[]{"loot", "prechecks", "bankPrechecks", "captain", "progress", "account", "pbs", "chat", "deaths", "pets", "milestones", "clues"}) { features.addProperty(f, true); }
		hello.add("features", features);
		hello.addProperty("clanName", "Ironclad");
		String view = args.length > 2 ? args[2] : "";
		final BufferedImage[] shot = new BufferedImage[1];
		SwingUtilities.invokeAndWait(() -> {
			IroncladPanel panel = new IroncladPanel(config, null, actions);
			panel.update(new SessionState(true, "Synced", Instant.now(), hello, sync), 0, actions);
			// Optional "previewBank": {key: card} draws bank answers waiting to be sent.
			if (sync.has("previewBank")) { java.util.Map<String, JsonObject> cards = new java.util.LinkedHashMap<>(); sync.getAsJsonObject("previewBank").entrySet().forEach(e -> cards.put(e.getKey(), e.getValue().getAsJsonObject())); panel.bank(cards); }
			if (!view.isEmpty()) { panel.showTab(view); }
			JComponent root = panel;
			int width = 242;
			root.setSize(width, 6000); layout(root);
			int height = Math.min(6000, Math.max(300, root.getPreferredSize().height));
			root.setSize(width, height); layout(root);
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			root.printAll(g); g.dispose(); shot[0] = image; panel.stop();
		});
		ImageIO.write(shot[0], "png", new java.io.File(args[1]));
		System.exit(0);
	}
}
