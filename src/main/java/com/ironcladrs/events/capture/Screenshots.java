package com.ironcladrs.events.capture;

import com.ironcladrs.events.PlayerGuard;
import com.ironcladrs.events.sync.SyncService;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.DrawManager;

/** Frames and JPEG output stay in memory; encoding never runs on the client thread. */
@Singleton
public class Screenshots
{
    @Inject private DrawManager drawManager;
    @Inject private WorkExecutor work;
    @Inject private ClientThread clientThread;
    @Inject private PlayerGuard guard;
    @Inject private SyncService sync;
    @Inject private com.ironcladrs.events.IroncladEventsConfig config;
    private int pending;
    private final List<Capture> captures = new ArrayList<>();
    public void capture(Consumer<byte[]> callback)
    {
        if (pending >= 3 || guard.identity() == null) { callback.accept(null); return; }
        long generation = guard.generation(); pending++;
        Capture capture = new Capture(generation, callback); captures.add(capture);
        capture.timeout = work.schedule(() -> clientThread.invoke(() -> finish(capture, null)), 5);
        int limit = sync.state().limit("screenshotBytes", 1_500_000, 64_000, 1_500_000);
        int scale = new com.ironcladrs.events.filters.Filters(config, sync.state()).screenshotScale;
        drawManager.requestNextFrameListener(frame -> {
            if (capture.done.get() || generation != guard.generation() || frame.getWidth(null) <= 0 || frame.getHeight(null) <= 0) { return; }
            // DrawManager hands listeners a stable frame; copy before retaining it for background work.
            BufferedImage copy = new BufferedImage(frame.getWidth(null), frame.getHeight(null), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = copy.createGraphics(); graphics.drawImage(frame, 0, 0, null); graphics.dispose();
            work.execute(() -> {
                byte[] result;
                try { result = encode(copy, limit, scale); } catch (Exception ex) { result = null; }
                final byte[] bytes = result;
                clientThread.invoke(() -> {
                    finish(capture, bytes);
                });
            });
        });
    }
    private void finish(Capture capture, byte[] bytes)
    {
        if (!capture.done.compareAndSet(false, true)) { return; }
        if (capture.timeout != null) { capture.timeout.cancel(false); }
        captures.remove(capture); pending = Math.max(0, pending - 1);
        if (capture.generation == guard.generation()) { capture.callback.accept(bytes); }
    }
    public void reset()
    {
        for (Capture capture : captures)
        { capture.done.set(true); if (capture.timeout != null) { capture.timeout.cancel(false); } }
        captures.clear(); pending = 0;
    }
    private static final class Capture
    {
        final long generation; final Consumer<byte[]> callback; final AtomicBoolean done = new AtomicBoolean();
        ScheduledFuture<?> timeout;
        Capture(long generation, Consumer<byte[]> callback) { this.generation = generation; this.callback = callback; }
    }
    public static byte[] encode(BufferedImage input, int limit) throws java.io.IOException { return encode(input, limit, 100); }
    /** JPEG at most `limit` bytes, shrunk to `percent` of the window first (Screenshot size). */
    public static byte[] encode(BufferedImage input, int limit, int percent) throws java.io.IOException
    {
        int width = Math.max(1, Math.min(1600, input.getWidth() * Math.max(25, Math.min(100, percent)) / 100));
        int height = Math.max(1, input.getHeight() * width / input.getWidth());
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics(); graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(input, 0, 0, width, height, null); graphics.dispose();
        for (float quality : new float[]{0.85f, 0.7f})
        {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) { return null; }
            ImageWriter writer = writers.next(); ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(buffer))
            {
                writer.setOutput(output); ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); param.setCompressionQuality(quality);
                writer.write(null, new IIOImage(scaled, null, null), param); output.flush();
            }
            finally { writer.dispose(); }
            if (buffer.size() <= limit) { return buffer.toByteArray(); }
        }
        return null; // Never exceed the advertised upload limit.
    }
}
