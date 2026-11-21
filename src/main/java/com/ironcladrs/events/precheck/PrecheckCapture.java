package com.ironcladrs.events.precheck;

import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.capture.Screenshots;
import com.ironcladrs.events.queue.*;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Capture and upload are separate user actions; preview bytes are not sent automatically. */
@Singleton
public class PrecheckCapture
{
    @Inject private Screenshots screenshots;
    @Inject private Consent consent;
    @Inject private Outbox outbox;
    public void capture(Consumer<byte[]> preview)
    { if (consent.allows("prechecks")) { screenshots.capture(preview); } else { preview.accept(null); } }
    public boolean send(long eventId, long precheckId, String value, String note, byte[] image)
    {
        if (image == null) { return false; }
        return outbox.submit("prechecks", "/precheck", Json.obj("eventId", eventId, "precheckId", precheckId,
            "value", value.isEmpty() ? null : value.substring(0, Math.min(40, value.length())),
            "note", note.substring(0, Math.min(500, note.length()))), image, ignored -> {});
    }
}
