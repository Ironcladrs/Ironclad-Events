package com.ironcladrs.events.queue;

import com.google.gson.*;
import com.ironcladrs.events.*;
import com.ironcladrs.events.api.ApiClient;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.notice.ChatNotices;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Memory-only queue, confined to the client thread. Entries never cross identity generations. */
@Singleton
public class Outbox
{
    public static final int CAPACITY = 30;
    @Inject private ApiClient api;
    @Inject private PlayerGuard guard;
    @Inject private Consent consent;
    @Inject private IroncladEventsConfig config;
    @Inject private ChatNotices notices;
    @Inject private com.ironcladrs.events.sync.SyncService sync;
    private final Deque<Entry> entries = new ArrayDeque<>();
    private long generation = -1;
    private Instant nextAttempt = Instant.EPOCH;
    private boolean busy, outage;
    private Consumer<String> status = ignored -> {};

    public void listen(Consumer<String> listener) { status = listener; }
    public int size() { return entries.size(); }
    public void clear() { entries.clear(); busy = false; outage = false; generation = guard.generation(); nextAttempt = Instant.EPOCH; }
    public boolean submit(String feature, String path, JsonObject data, byte[] screenshot, Consumer<ApiClient.Result> callback)
    {
        if (!consent.allows(feature)) { status.accept("Enable this upload in settings first, and check the connection."); return false; }
        if (generation != guard.generation()) { clear(); }
        if (entries.size() >= CAPACITY) { status.accept("Upload queue full. Please use the site for this submission."); return false; }
        JsonObject payload = data.deepCopy();
        if (!payload.has("idempotencyKey")) { payload.addProperty("idempotencyKey", UUID.randomUUID().toString()); }
        entries.add(new Entry(feature, path, payload, screenshot, Instant.now(), callback));
        pump(Instant.now()); return true;
    }
    public void pump(Instant now)
    {
        if (generation != guard.generation()) { clear(); }
        if (busy || entries.isEmpty() || now.isBefore(nextAttempt)) { return; }
        Entry entry = entries.peek();
        if (!consent.allows(entry.feature) || now.isAfter(entry.created.plusSeconds(900)))
        { entries.remove(); status.accept("An unsent upload was discarded after consent changed or it expired."); return; }
        if (now.isBefore(entry.due)) { return; }
        if (entry.attempts >= 3 && (sync.state().syncedAt == null || sync.state().syncedAt.isBefore(entry.due))) { return; }
        busy = true; long expected = generation;
        // Each feature has its own screenshot switch, re-read at send time; precheck pictures are always asked for.
        boolean pictures = "deaths".equals(entry.feature) ? config.deathScreenshots() : "pbs".equals(entry.feature) ? config.pbScreenshots()
            : "loot".equals(entry.feature) ? config.screenshots() : "prechecks".equals(entry.feature);
        byte[] image = pictures ? entry.image : null;
        if ("/precheck".equals(entry.path) && image == null) { busy = false; entries.remove(); return; }
        api.request(guard.identity(), "POST", entry.path, entry.payload, image, null, result -> {
            if (expected != guard.generation()) { return; } busy = false;
            if (result.success())
            {
                entries.remove(entry); outage = false; entry.callback.accept(result);
                status.accept(result.status == 409 ? "Already received by the site." : "Received by the site.");
            }
            else if (result.retryable())
            {
                entry.attempts++; entry.due = Instant.now().plusSeconds(Math.max(delay(entry.attempts), result.status == 429 ? result.retryAfter() : 0));
                nextAttempt = entry.due;
                if (!outage) { outage = true; notices.queued(entries.size()); }
                status.accept("Couldn't reach the site; " + entries.size() + " upload(s) queued.");
            }
            else { entries.remove(entry); status.accept(result.message()); entry.callback.accept(result); }
        });
    }
    public static int delay(int attempt) { return attempt <= 1 ? 5 : attempt == 2 ? 15 : 60; }
    private static final class Entry
    {
        final String feature, path; final JsonObject payload; final byte[] image;
        final Instant created; final Consumer<ApiClient.Result> callback;
        Instant due = Instant.EPOCH; int attempts;
        Entry(String feature, String path, JsonObject payload, byte[] image, Instant created, Consumer<ApiClient.Result> callback)
        { this.feature = feature; this.path = path; this.payload = payload; this.image = image; this.created = created; this.callback = callback; }
    }
}
