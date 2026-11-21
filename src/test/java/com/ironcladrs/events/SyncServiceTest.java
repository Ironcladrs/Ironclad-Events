package com.ironcladrs.events;

import com.google.gson.*;
import com.ironcladrs.events.api.ApiClient;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.notice.ChatNotices;
import com.ironcladrs.events.sync.SyncService;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SyncServiceTest
{
    private SyncService sync;
    private PlayerGuard guard;
    private final List<String> paths = new ArrayList<>();
    private Consumer<ApiClient.Result> callback;
    @Before public void setup() throws Exception
    {
        sync = new SyncService(); guard = mock(PlayerGuard.class); ApiClient api = mock(ApiClient.class);
        when(guard.identity()).thenReturn(TestSupport.identity()); when(guard.generation()).thenReturn(1L);
        TestSupport.inject(sync, "guard", guard); TestSupport.inject(sync, "api", api);
        TestSupport.inject(sync, "config", new IroncladEventsConfig() { @Override public boolean connect() { return true; } });
        TestSupport.inject(sync, "notices", mock(ChatNotices.class));
        doAnswer(call -> { paths.add(call.getArgument(2)); callback = call.getArgument(6); return null; })
            .when(api).request(any(), anyString(), anyString(), any(), nullable(byte[].class), nullable(String.class), any());
    }
    @Test public void helloThenSyncThen304RetainsEvents()
    {
        sync.tick(Instant.now()); assertEquals(Collections.singletonList("/hello"), paths);
        callback.accept(new ApiClient.Result(200, Json.obj("minVersion", "1.0.0", "features", Json.obj("loot", true)), "", true));
        assertEquals("/sync", paths.get(1));
        JsonArray events = new JsonArray(); events.add(Json.obj("id", 91));
        callback.accept(new ApiClient.Result(200, Json.obj("cursor", "n:1", "events", events), "etag", true));
        sync.tick(Instant.now().plusSeconds(601)); callback.accept(new ApiClient.Result(304, new JsonObject(), "", true));
        assertEquals(1, sync.state().events().size()); assertTrue(sync.state().linked);
    }
    @Test public void logoutStopsPollingAndStaleCallbacks()
    {
        sync.tick(Instant.now()); Consumer<ApiClient.Result> old = callback;
        when(guard.identity()).thenReturn(null); when(guard.generation()).thenReturn(2L); sync.tick(Instant.now().plusSeconds(1000));
        old.accept(new ApiClient.Result(200, new JsonObject(), "", true)); assertFalse(sync.state().linked); assertEquals(1, paths.size());
    }
    @Test public void permanentAuthFailureDoesNotLoop()
    {
        sync.tick(Instant.now()); callback.accept(new ApiClient.Result(401, Json.obj("message", "Invalid key."), "", true));
        sync.tick(Instant.now().plusSeconds(3600)); assertEquals(1, paths.size()); assertEquals("Invalid key.", sync.state().message);
    }
    @Test public void exponentialBackoffAndUpgradeChecks()
    {
        assertEquals(20, SyncService.backoff(1)); assertEquals(40, SyncService.backoff(2)); assertEquals(600, SyncService.backoff(100));
        assertTrue(SyncService.compareVersions("1.0.0", "1.1.0") < 0); assertEquals(0, SyncService.compareVersions("1.0.0", "1.0"));
        sync.tick(Instant.now()); callback.accept(new ApiClient.Result(200, Json.obj("minVersion", "2.0.0"), "", true)); assertFalse(sync.state().linked); assertEquals(1, paths.size());
    }
    @Test public void serverSwitchChangesApplyDuringSync()
    {
        sync.tick(Instant.now());
        callback.accept(new ApiClient.Result(200, Json.obj("minVersion", "1.0.0", "features", Json.obj("loot", true)), "", true));
        callback.accept(new ApiClient.Result(200, Json.obj("features", Json.obj("loot", false), "etag", "bad\r\nheader"), "", true));
        assertFalse(sync.state().feature("loot"));
        sync.tick(Instant.now().plusSeconds(601));
        callback.accept(new ApiClient.Result(200, Json.obj("features", Json.obj("loot", true)), "", true));
        assertTrue(sync.state().feature("loot"));
    }

}
