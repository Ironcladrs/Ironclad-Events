package com.ironcladrs.events;

import com.google.gson.Gson;
import com.ironcladrs.events.api.ApiClient;
import com.ironcladrs.events.api.dto.Json;
import java.util.concurrent.*;
import okhttp3.*;
import okhttp3.mockwebserver.*;
import org.junit.*;
import static org.junit.Assert.*;

public class ApiClientTest
{
    private MockWebServer server;
    private ApiClient api;
    @Before public void setup() throws Exception
    {
        server = new MockWebServer(); server.start(); api = new ApiClient();
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> {
            HttpUrl original = chain.request().url();
            assertEquals("https", original.scheme()); assertEquals("ironcladrs.com", original.host());
            Request rewritten = chain.request().newBuilder().url(server.url(original.encodedPath() + (original.encodedQuery() == null ? "" : "?" + original.encodedQuery()))).build();
            return chain.proceed(rewritten);
        }).build();
        TestSupport.inject(api, "http", transport); TestSupport.inject(api, "gson", new Gson()); TestSupport.inject(api, "clientThread", TestSupport.immediateThread());
    }
    @After public void teardown() throws Exception { api.cancelAll(); server.shutdown(); }
    private ApiClient.Result request(String method, String path) throws Exception
    {
        CompletableFuture<ApiClient.Result> result = new CompletableFuture<>();
        api.request(TestSupport.identity(), method, path, Json.obj("idempotencyKey", "fixture", "since", "n:1"), null, "v1", result::complete);
        return result.get(5, TimeUnit.SECONDS);
    }
    @Test public void bearerOnlyInHeaderAndIdentityOnReads() throws Exception
    {
        server.enqueue(new MockResponse().setBody("{}")); assertTrue(request("GET", "/sync").success());
        RecordedRequest sent = server.takeRequest(5, TimeUnit.SECONDS);
        assertEquals("Bearer " + TestSupport.identity().key(), sent.getHeader("Authorization"));
        assertFalse(sent.getPath().contains(TestSupport.identity().key())); assertTrue(sent.getPath().contains("accountHash=123"));
        assertEquals("v1", sent.getHeader("If-None-Match")); assertEquals("1.0.0", sent.getHeader("X-Ironclad-Plugin"));
    }
    @Test public void redirectToAnotherHostIsNeverFollowed() throws Exception
    {
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "https://example.invalid/stolen"));
        assertEquals(302, request("POST", "/hello").status); assertEquals(1, server.getRequestCount());
    }
    @Test public void mapsRetryAndPermanentErrors() throws Exception
    {
        server.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", "17").setBody("{\"error\":\"slow_down\",\"message\":\"Please wait.\"}"));
        ApiClient.Result retry = request("POST", "/hello"); assertTrue(retry.retryable()); assertEquals(17, retry.retryAfter()); assertEquals("Please wait.", retry.message());
        server.enqueue(new MockResponse().setResponseCode(403).setBody("{\"error\":\"rsn_not_linked\",\"message\":\"Link this account.\"}"));
        ApiClient.Result rejected = request("POST", "/hello"); assertFalse(rejected.retryable()); assertEquals("Link this account.", rejected.message());
    }
    @Test public void multipartHasPayloadAndOptionalImage() throws Exception
    {
        server.enqueue(new MockResponse().setBody("{}")); request("POST", "/loot");
        RecordedRequest sent = server.takeRequest(5, TimeUnit.SECONDS); String body = sent.getBody().readUtf8();
        assertTrue(body.contains("name=\"payload\"")); assertTrue(body.contains("accountHash")); assertFalse(body.contains("name=\"screenshot\""));
    }
    @Test public void onlyDuplicateConflictCountsAsAnAcceptedUpload()
    {
        assertTrue(new ApiClient.Result(409, Json.obj("error", "duplicate"), "", true).success());
        assertFalse(new ApiClient.Result(409, Json.obj("error", "event_locked"), "", true).success());
    }

}
