package com.ironcladrs.events.api;

import com.google.gson.*;
import com.ironcladrs.events.PlayerGuard;
import com.ironcladrs.events.api.dto.Json;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.callback.ClientThread;
import okhttp3.*;

/** All outbound traffic is fixed to this HTTPS origin. Redirects are never followed. */
@Singleton
public class ApiClient
{
    public static final String BASE_URL = "https://ironcladrs.com/ingest/plugin/v1";
    public static final String VERSION = "1.0.0";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_RESPONSE_BYTES = 2_000_000;
    @Inject private OkHttpClient http;
    @Inject private Gson gson;
    @Inject private ClientThread clientThread;
    private final Set<Call> calls = ConcurrentHashMap.newKeySet();
    private OkHttpClient transport;

    public void request(PlayerGuard.Identity identity, String method, String path, JsonObject payload,
                        byte[] image, String etag, Consumer<Result> callback)
    {
        if (identity == null) { callback.accept(new Result(0, Json.obj("message", "Log in and connect first."), "", false)); return; }
        if (!path.matches("/(hello|sync|loot|death|activity|precheck|precheck/attest|progress|account|pb|chat|session|recommendations/[0-9]+/response)"))
        { throw new IllegalArgumentException("Unsupported plugin endpoint"); }
        if (transport == null) { transport = http.newBuilder().followRedirects(false).followSslRedirects(false).build(); }
        HttpUrl.Builder url = HttpUrl.parse(BASE_URL + path).newBuilder();
        RequestBody body = null;
        JsonObject data = identity.payload();
        if (payload != null) { payload.entrySet().forEach(entry -> data.add(entry.getKey(), entry.getValue().deepCopy())); }
        if ("GET".equals(method))
        {
            url.addQueryParameter("rsn", identity.rsn).addQueryParameter("accountHash", Long.toString(identity.accountHash));
            url.addQueryParameter("worldTypes", data.get("worldTypes").toString());
            if (payload != null && payload.has("since")) { url.addQueryParameter("since", Json.text(payload, "since")); }
        }
        else if (!"DELETE".equals(method))
        {
            String json = gson.toJson(data);
            boolean multipart = path.equals("/loot") || path.equals("/death") || path.equals("/precheck") || path.equals("/pb");
            if (multipart)
            {
                MultipartBody.Builder parts = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("payload", json);
                if (image != null) { parts.addFormDataPart("screenshot", "capture.jpg", RequestBody.create(MediaType.parse("image/jpeg"), image)); }
                body = parts.build();
            }
            else { body = RequestBody.create(JSON, json); }
        }
        Request.Builder request = new Request.Builder().url(url.build()).method(method, body)
            .header("Authorization", "Bearer " + identity.key()).header("X-Ironclad-Plugin", VERSION)
            .header("X-Ironclad-Rsn", identity.rsn).header("X-Ironclad-Account-Hash", Long.toString(identity.accountHash))
            .header("X-Ironclad-World-Types", identity.payload().get("worldTypes").toString());
        if (etag != null && !etag.isEmpty()) { request.header("If-None-Match", etag); }
        Call call = transport.newCall(request.build()); calls.add(call);
        call.enqueue(new Callback()
        {
            @Override public void onFailure(Call failed, java.io.IOException ex)
            { complete(failed, new Result(0, Json.obj("message", "Couldn't reach ironcladrs.com."), "", false)); }
            @Override public void onResponse(Call finished, Response response)
            {
                try (Response closeable = response)
                {
                    JsonObject result = new JsonObject();
                    if (response.code() != 304 && response.body() != null)
                    {
                        okio.BufferedSource source = response.body().source(); source.request(MAX_RESPONSE_BYTES + 1L);
                        if (source.buffer().size() > MAX_RESPONSE_BYTES) { throw new java.io.IOException("Response too large"); }
                        String text = source.readUtf8();
                        if (!text.isEmpty()) { result = gson.fromJson(text, JsonObject.class); }
                    }
                    if (result == null) { result = new JsonObject(); }
                    String retry = response.header("Retry-After");
                    if (retry != null && retry.matches("[0-9]+")) { result.addProperty("retryAfterSec", retry); }
                    complete(finished, new Result(response.code(), result, response.header("ETag", ""), true));
                }
                catch (Exception ex) { complete(finished, new Result(0, Json.obj("message", "The site returned an unreadable response."), "", false)); }
            }
            private void complete(Call finished, Result result)
            {
                calls.remove(finished);
                if (!finished.isCanceled()) { clientThread.invoke(() -> callback.accept(result)); }
            }
        });
    }
    public void cancelAll() { calls.forEach(Call::cancel); calls.clear(); }
    public static final class Result
    {
        public final int status;
        public final JsonObject data;
        public final String etag;
        public final boolean received;
        public Result(int status, JsonObject data, String etag, boolean received)
        { this.status = status; this.data = data; this.etag = etag; this.received = received; }
        public boolean success() { return status >= 200 && status < 300 || status == 409 && "duplicate".equals(Json.text(data, "error")); }
        public boolean retryable() { return status == 0 || status == 429 || status >= 500 && status <= 599; }
        public int retryAfter() { return Json.bounded(data, "retryAfterSec", 5, 1, 600); }
        public String message()
        {
            String text = Json.text(data, "message");
            return text.isEmpty() ? "The site could not accept this request (" + status + ")." : text.substring(0, Math.min(500, text.length()));
        }
    }
}
