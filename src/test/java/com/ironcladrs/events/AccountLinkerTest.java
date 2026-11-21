package com.ironcladrs.events;

import com.google.gson.Gson;
import com.ironcladrs.events.capture.WorkExecutor;
import com.ironcladrs.events.sync.AccountLinker;
import java.util.concurrent.*;
import net.runelite.client.config.ConfigManager;
import okhttp3.*;
import okhttp3.mockwebserver.*;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** The link flow against a local server: the key reaches the setting only after approval, and only from the clan site. */
public class AccountLinkerTest
{
    private static final String KEY = "ice_" + "A".repeat(43);
    private MockWebServer server;
    private WorkExecutor work;
    private ConfigManager configs;
    private final BlockingQueue<AccountLinker.State> states = new LinkedBlockingQueue<>();

    private AccountLinker linker(boolean connect) throws Exception
    {
        AccountLinker linker = new AccountLinker();
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> {
            HttpUrl original = chain.request().url();
            assertEquals("https", original.scheme()); assertEquals("ironcladrs.com", original.host());
            return chain.proceed(chain.request().newBuilder().url(server.url(original.encodedPath())).build());
        }).build();
        TestSupport.inject(linker, "http", transport); TestSupport.inject(linker, "gson", new Gson());
        TestSupport.inject(linker, "clientThread", TestSupport.immediateThread()); TestSupport.inject(linker, "configManager", configs);
        TestSupport.inject(linker, "config", new IroncladEventsConfig() { @Override public boolean connect() { return connect; } });
        TestSupport.inject(linker, "work", work);
        linker.listen(states::add); states.clear(); return linker;
    }
    private AccountLinker.State until(AccountLinker.State.Phase phase) throws Exception
    {
        long end = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < end)
        { AccountLinker.State next = states.poll(500, TimeUnit.MILLISECONDS); if (next != null && next.phase == phase) { return next; } }
        fail("Never reached " + phase); return null;
    }
    private static MockResponse json(int code, String body) { return new MockResponse().setResponseCode(code).setBody(body); }
    private static String started(String url)
    { return "{\"code\":\"ABCD-2345\",\"secret\":\"s3cret\",\"url\":\"" + url + "\",\"pollEvery\":2}"; }

    @Before public void setup() throws Exception { server = new MockWebServer(); server.start(); work = new WorkExecutor(); work.start(); configs = mock(ConfigManager.class); }
    @After public void teardown() throws Exception { work.stop(); server.shutdown(); }

    @Test public void approvedLinkSavesTheKeyIntoTheSetting() throws Exception
    {
        AccountLinker linker = linker(true);
        server.enqueue(json(201, started("https://ironcladrs.com/plugin?code=ABCD-2345")));
        server.enqueue(json(200, "{\"status\":\"pending\"}"));
        server.enqueue(json(200, "{\"status\":\"approved\",\"key\":\"" + KEY + "\"}"));
        linker.start();
        AccountLinker.State waiting = until(AccountLinker.State.Phase.WAITING);
        assertEquals("ABCD-2345", waiting.code); assertEquals("https://ironcladrs.com/plugin?code=ABCD-2345", waiting.url);
        verify(configs, never()).setConfiguration(anyString(), anyString(), any());
        until(AccountLinker.State.Phase.LINKED);
        verify(configs).setConfiguration(IroncladEventsConfig.GROUP, "apiKey", KEY);
        assertEquals("/api/plugin-link/start", server.takeRequest().getPath());
        RecordedRequest poll = server.takeRequest();
        assertEquals("/api/plugin-link/poll", poll.getPath()); assertTrue(poll.getBody().readUtf8().contains("\"secret\":\"s3cret\""));
    }
    @Test public void nothingIsSentUntilConnectIsOn() throws Exception
    {
        linker(false).start();
        assertTrue(until(AccountLinker.State.Phase.FAILED).message.contains("Connect"));
        assertEquals(0, server.getRequestCount());
    }
    @Test public void aCodeForAnotherSiteIsRefused() throws Exception
    {
        server.enqueue(json(201, started("https://evil.test/plugin")));
        linker(true).start();
        until(AccountLinker.State.Phase.FAILED);
        assertEquals(1, server.getRequestCount()); verify(configs, never()).setConfiguration(anyString(), anyString(), any());
    }
    @Test public void anExpiredCodeAsksForANewOne() throws Exception
    {
        server.enqueue(json(201, started("https://ironcladrs.com/plugin?code=ABCD-2345")));
        server.enqueue(json(200, "{\"status\":\"expired\"}"));
        linker(true).start();
        assertTrue(until(AccountLinker.State.Phase.FAILED).message.contains("expired"));
        verify(configs, never()).setConfiguration(anyString(), anyString(), any());
    }
    @Test public void cancellingStopsPolling() throws Exception
    {
        AccountLinker linker = linker(true);
        server.enqueue(json(201, started("https://ironcladrs.com/plugin?code=ABCD-2345")));
        linker.start(); until(AccountLinker.State.Phase.WAITING); linker.cancel();
        assertEquals(AccountLinker.State.Phase.IDLE, linker.state().phase); states.clear();
        assertNull("No further progress after cancelling", states.poll(3, TimeUnit.SECONDS));
        assertEquals("Only the start request was made", 1, server.getRequestCount());
    }
}
