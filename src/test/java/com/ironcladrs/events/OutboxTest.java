package com.ironcladrs.events;

import com.google.gson.*;
import com.ironcladrs.events.api.ApiClient;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.notice.ChatNotices;
import com.ironcladrs.events.queue.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class OutboxTest
{
    private Outbox outbox;
    private ApiClient api;
    private Consent consent;
    private PlayerGuard guard;
    private final List<JsonObject> sent = new ArrayList<>();
    private Consumer<ApiClient.Result> callback;
    @Before public void setup() throws Exception
    {
        outbox = new Outbox(); api = mock(ApiClient.class); consent = mock(Consent.class); guard = mock(PlayerGuard.class);
        when(consent.allows(anyString())).thenReturn(true); when(guard.identity()).thenReturn(TestSupport.identity()); when(guard.generation()).thenReturn(1L);
        TestSupport.inject(outbox, "api", api); TestSupport.inject(outbox, "consent", consent); TestSupport.inject(outbox, "guard", guard);
        TestSupport.inject(outbox, "config", new IroncladEventsConfig() {}); TestSupport.inject(outbox, "notices", mock(ChatNotices.class));
        doAnswer(call -> { sent.add(call.getArgument(3, JsonObject.class).deepCopy()); callback = call.getArgument(6); return null; })
            .when(api).request(any(), anyString(), anyString(), any(), nullable(byte[].class), nullable(String.class), any());
    }
    @Test public void retryReusesIdempotencyKeyAndPayload()
    {
        assertTrue(outbox.submit("loot", "/loot", Json.obj("occurredAt", "fixed"), null, ignored -> {}));
        callback.accept(new ApiClient.Result(503, new JsonObject(), "", true));
        outbox.pump(Instant.now().plusSeconds(6)); assertEquals(2, sent.size()); assertEquals(sent.get(0), sent.get(1));
        callback.accept(new ApiClient.Result(200, new JsonObject(), "", true)); assertEquals(0, outbox.size());
    }
    @Test public void boundedAndNoUploadWithoutConsent()
    {
        for (int i = 0; i < 30; i++) { assertTrue(outbox.submit("loot", "/loot", new JsonObject(), null, ignored -> {})); }
        assertFalse(outbox.submit("loot", "/loot", new JsonObject(), null, ignored -> {})); assertEquals(30, outbox.size());
        when(guard.generation()).thenReturn(2L); when(consent.allows(anyString())).thenReturn(false);
        outbox.pump(Instant.now()); assertEquals(0, outbox.size()); assertEquals(1, sent.size());
        assertFalse(outbox.submit("loot", "/loot", new JsonObject(), null, ignored -> {})); assertEquals(1, sent.size());
    }
    @Test public void oldAccountCallbackCannotAlterNewQueue()
    {
        outbox.submit("loot", "/loot", new JsonObject(), null, ignored -> fail("Old callback invoked"));
        Consumer<ApiClient.Result> old = callback; when(guard.generation()).thenReturn(2L); outbox.clear();
        outbox.submit("loot", "/loot", new JsonObject(), null, ignored -> {});
        old.accept(new ApiClient.Result(200, new JsonObject(), "", true)); assertEquals(1, outbox.size());
    }
    @Test public void nonRetryableFailureIsDropped()
    { outbox.submit("loot", "/loot", new JsonObject(), null, ignored -> {}); callback.accept(new ApiClient.Result(403, new JsonObject(), "", true)); assertEquals(0, outbox.size()); }
    @Test public void deathImagesHaveIndependentConsentFromDropImages() throws Exception
    {
        TestSupport.inject(outbox,"config",new IroncladEventsConfig() { @Override public boolean deathScreenshots(){return true;} });
        byte[] image = new byte[]{1,2,3};
        assertTrue(outbox.submit("deaths","/death",Json.obj("at",Instant.now()),image,ignored -> {}));
        verify(api).request(any(),eq("POST"),eq("/death"),any(),eq(image),isNull(),any());
    }

}
