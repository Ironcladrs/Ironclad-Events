package com.ironcladrs.events.sync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ironcladrs.events.IroncladEventsConfig;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.capture.WorkExecutor;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import okhttp3.*;

/**
 * Links this plugin to a member's account without copying a key. Only runs while Connect is on, so the
 * third-party warning has been accepted first. The site hands the key to whoever holds the private poll
 * secret, which never leaves this object; the key goes straight into the Plugin key setting.
 */
@Slf4j
@Singleton
public class AccountLinker
{
    public static final String BASE_URL = "https://ironcladrs.com/api/plugin-link";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_RESPONSE_BYTES = 10_000;

    /** What the panel shows. Immutable so the Swing thread can read it safely. */
    public static final class State
    {
        public enum Phase { IDLE, STARTING, WAITING, LINKED, FAILED }
        public final Phase phase; public final String code, url, message;
        public State(Phase phase, String code, String url, String message) { this.phase = phase; this.code = code; this.url = url; this.message = message; }
        public static final State IDLE_STATE = new State(Phase.IDLE, "", "", "");
    }

    @Inject private OkHttpClient http;
    @Inject private Gson gson;
    @Inject private ClientThread clientThread;
    @Inject private ConfigManager configManager;
    @Inject private IroncladEventsConfig config;
    @Inject private WorkExecutor work;
    private final Set<Call> calls = ConcurrentHashMap.newKeySet();
    private volatile Consumer<State> listener = state -> {};
    private volatile State state = State.IDLE_STATE;
    private volatile ScheduledFuture<?> next;
    private volatile String secret = "";
    private volatile long attempt;

    public void listen(Consumer<State> listener) { this.listener = listener; listener.accept(state); }
    public State state() { return state; }

    // ── Start, poll, finish ──────────────────────────────────────────────────
    public void start()
    {
        if (!config.connect()) { publish(new State(State.Phase.FAILED, "", "", "Turn on Connect to ironcladrs.com in the plugin's settings first.")); return; }
        cancel(); long mine = ++attempt;
        publish(new State(State.Phase.STARTING, "", "", "Asking ironcladrs.com for a link code..."));
        post("/start", new JsonObject(), mine, result -> {
            String code = Json.text(result, "code"), url = Json.text(result, "url");
            secret = Json.text(result, "secret");
            if (code.isEmpty() || secret.isEmpty() || !url.startsWith("https://ironcladrs.com/"))
            { fail(mine, Json.text(result, "message").isEmpty() ? "Couldn't start linking. Try again in a minute." : Json.text(result, "message")); return; }
            publish(new State(State.Phase.WAITING, code, url, "Approve this code on the page that just opened in your browser."));
            schedulePoll(mine, Json.bounded(result, "pollEvery", 3, 2, 30));
        });
    }
    private void schedulePoll(long mine, int seconds)
    {
        if (mine != attempt) { return; }
        next = work.schedule(() -> poll(mine, seconds), seconds);
    }
    private void poll(long mine, int seconds)
    {
        State now = state; if (mine != attempt || now.phase != State.Phase.WAITING) { return; }
        post("/poll", Json.obj("code", now.code, "secret", secret), mine, result -> {
            String status = Json.text(result, "status"), key = Json.text(result, "key");
            if ("approved".equals(status) && key.matches("ice_[A-Za-z0-9_-]{43}"))
            {
                secret = "";
                // Saving the key fires ConfigChanged, which reconnects with it.
                clientThread.invoke(() -> configManager.setConfiguration(IroncladEventsConfig.GROUP, "apiKey", key));
                publish(new State(State.Phase.LINKED, now.code, "", "Linked. Connecting to your account..."));
            }
            else if ("pending".equals(status)) { schedulePoll(mine, seconds); }
            else { fail(mine, "That code expired. Press Link my account to get a new one."); }
        });
    }
    public void cancel()
    {
        attempt++; secret = "";
        ScheduledFuture<?> pending = next; if (pending != null) { pending.cancel(false); next = null; }
        calls.forEach(Call::cancel); calls.clear();
        if (state.phase != State.Phase.IDLE) { publish(State.IDLE_STATE); }
    }

    // ── Plumbing ─────────────────────────────────────────────────────────────
    private void fail(long mine, String message)
    { if (mine == attempt) { secret = ""; publish(new State(State.Phase.FAILED, "", "", message)); } }
    private void publish(State next) { state = next; listener.accept(next); }
    private void post(String path, JsonObject body, long mine, Consumer<JsonObject> success)
    {
        OkHttpClient transport = http.newBuilder().followRedirects(false).followSslRedirects(false).build();
        Request request = new Request.Builder().url(BASE_URL + path).post(RequestBody.create(JSON, gson.toJson(body))).build();
        Call call = transport.newCall(request); calls.add(call);
        call.enqueue(new Callback()
        {
            @Override public void onFailure(Call failed, IOException ex)
            { calls.remove(failed); if (!failed.isCanceled()) { fail(mine, "Couldn't reach ironcladrs.com. Check your connection and try again."); } }
            @Override public void onResponse(Call finished, Response response)
            {
                calls.remove(finished);
                try (Response closeable = response)
                {
                    if (finished.isCanceled() || mine != attempt) { return; }
                    JsonObject result = new JsonObject();
                    if (response.body() != null)
                    {
                        okio.BufferedSource source = response.body().source(); source.request(MAX_RESPONSE_BYTES + 1L);
                        if (source.buffer().size() > MAX_RESPONSE_BYTES) { throw new IOException("Response too large"); }
                        String text = source.readUtf8(); if (!text.isEmpty()) { JsonObject parsed = gson.fromJson(text, JsonObject.class); if (parsed != null) { result = parsed; } }
                    }
                    if (response.code() == 429) { fail(mine, "Too many link attempts. Wait a few minutes, then try again."); return; }
                    if (response.code() >= 400) { fail(mine, Json.text(result, "message").isEmpty() ? "Linking failed (" + response.code() + "). Try again." : Json.text(result, "message")); return; }
                    success.accept(result);
                }
                catch (Exception ex) { log.debug("Link response unreadable", ex); fail(mine, "The site returned an unreadable answer. Try again."); }
            }
        });
    }
}
