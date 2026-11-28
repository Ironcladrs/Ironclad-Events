package com.ironcladrs.events.sync;

import com.google.gson.*;
import com.ironcladrs.events.*;
import com.ironcladrs.events.api.ApiClient;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.notice.ChatNotices;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Hello binds a session before any uploads. Stale callbacks cannot publish into a new account. */
@Singleton
public class SyncService
{
    @Inject private PlayerGuard guard;
    @Inject private IroncladEventsConfig config;
    @Inject private ApiClient api;
    @Inject private ChatNotices notices;
    @Inject private com.ironcladrs.events.filters.RarityTable rarity;
    private SessionState state = SessionState.empty("Connect in settings to get started.");
    private Consumer<SessionState> listener = ignored -> {};
    private boolean busy, blocked;
    private long generation = -1;
    private Instant next = Instant.EPOCH, lastManual = Instant.EPOCH, lastRead = Instant.EPOCH;
    private int failures;
    private String cursor = "", etag = "";

    public SessionState state() { return state; }
    public boolean current() { return state.linked && generation == guard.generation(); }
    public void listen(Consumer<SessionState> listener) { this.listener = listener; listener.accept(state); }
    public void reset()
    {
        generation = guard.generation(); busy = false; blocked = false; failures = 0;
        cursor = ""; etag = ""; next = Instant.EPOCH; lastRead = Instant.EPOCH;
        notices.reset();
        publish(SessionState.empty(!config.connect() ? "Connect in settings to get started."
            : guard.identity() == null ? "Log in on a standard world with a valid plugin key." : "Connecting…"));
    }
    public void tick(Instant now)
    {
        guard.refresh();
        if (generation != guard.generation()) { reset(); }
        if (guard.identity() == null || blocked || busy || now.isBefore(next)) { return; }
        if (!state.linked) { hello(now); } else { sync(now); }
    }
    public void manual(Instant now)
    {
        if (now.isBefore(lastManual.plusSeconds(10)) || now.isBefore(lastRead.plusSeconds(20))) { return; }
        lastManual = now; next = now; tick(now);
    }
    private void hello(Instant now)
    {
        JsonObject switches = Json.obj("connect", config.connect());
        for (String key : IroncladEventsConfig.UPLOAD_KEYS) { switches.addProperty(key, IroncladEventsConfig.upload(config, key)); }
        JsonObject payload = Json.obj("pluginVersion", ApiClient.VERSION, "switches", switches, "idempotencyKey", UUID.randomUUID());
        long expected = generation; busy = true;
        api.request(guard.identity(), "POST", "/hello", payload, null, null, result -> {
            if (expected != guard.generation()) { return; } busy = false;
            if (!result.success()) { failure(result, now); return; }
            if (compareVersions(ApiClient.VERSION, Json.text(result.data, "minVersion")) < 0)
            { blocked = true; publish(SessionState.empty("Update Ironclad Events to connect.")); return; }
            failures = 0; next = Instant.EPOCH;
            JsonObject hello = result.data.deepCopy(); rarity.update(Json.object(hello, "rarity")); hello.remove("rarity");
            publish(new SessionState(true, "Connected", null, hello, new JsonObject()));
            tick(Instant.now());
        });
    }
    private void sync(Instant now)
    {
        long expected = generation; busy = true; lastRead = now;
        api.request(guard.identity(), "GET", "/sync", Json.obj("since", cursor), null, etag, result -> {
            if (expected != guard.generation()) { return; } busy = false;
            if (result.status == 304)
            {
                failures = 0; next = Instant.now().plusSeconds(interval(Instant.now()));
                publish(new SessionState(true, "Connected", Instant.now(), state.hello(), state.sync())); return;
            }
            if (!result.success()) { failure(result, Instant.now()); return; }
            failures = 0;
            cursor = Json.text(result.data, "cursor"); etag = result.etag.isEmpty() ? Json.text(result.data, "etag") : result.etag;
            if (!etag.matches("[\\x20-\\x7E]{0,200}")) { etag = ""; }
            JsonObject hello = state.hello();
            if (result.data.has("features")) { hello.add("features", Json.object(result.data, "features")); }
            publish(new SessionState(true, "Connected", Instant.now(), hello, result.data));
            notices.accept(Json.array(result.data, "notices"));
            next = Instant.now().plusSeconds(interval(Instant.now()));
        });
    }
    private void failure(ApiClient.Result result, Instant now)
    {
        failures++;
        next = now.plusSeconds(Math.max(result.status == 429 ? result.retryAfter() : 0, backoff(failures)));
        if (!result.retryable()) { blocked = true; }
        boolean linked = state.linked && result.retryable();
        publish(new SessionState(linked, result.message(), state.syncedAt, state.hello(), state.sync()));
    }
    private int interval(Instant now)
    {
        boolean active = state.hasLiveEvent(now);
        for (JsonElement value : state.events())
        {
            if (!value.isJsonObject()) { continue; }
            Instant start = Json.time(Json.text(value.getAsJsonObject(), "startsAt"));
            if (start != null && start.isAfter(now) && !start.isAfter(now.plusSeconds(3600))) { active = true; }
        }
        return Json.bounded(Json.object(state.hello(), "poll"), active ? "liveSec" : "idleSec", active ? 60 : 600, 20, 3600);
    }
    public static int backoff(int failures) { return Math.min(600, 20 * (1 << Math.min(5, Math.max(0, failures - 1)))); }
    public static int compareVersions(String left, String right)
    {
        if (!right.matches("[0-9]+(?:\\.[0-9]+){0,3}")) { return right.isEmpty() ? 0 : -1; }
        String[] a = left.split("\\."), b = right.split("\\.");
        try { for (int i = 0; i < Math.max(a.length, b.length); i++)
        { int cmp = Integer.compare(i < a.length ? Integer.parseInt(a[i]) : 0, i < b.length ? Integer.parseInt(b[i]) : 0); if (cmp != 0) { return cmp; } } }
        catch (NumberFormatException ex) { return -1; }
        return 0;
    }
    private void publish(SessionState nextState) { state = nextState; listener.accept(nextState); }
}
