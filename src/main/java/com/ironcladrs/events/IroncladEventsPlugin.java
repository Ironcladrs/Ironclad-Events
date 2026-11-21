package com.ironcladrs.events;

import com.google.inject.Provides;
import com.ironcladrs.events.account.AccountSnapshot;
import com.ironcladrs.events.activity.ActivityCollector;
import com.ironcladrs.events.death.DeathListener;
import com.ironcladrs.events.api.ApiClient;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.capture.*;
import com.ironcladrs.events.chat.ClanChatRelay;
import com.ironcladrs.events.clog.*;
import com.ironcladrs.events.loot.LootCollector;
import com.ironcladrs.events.pb.PersonalBestListener;
import com.ironcladrs.events.precheck.*;
import com.ironcladrs.events.progress.*;
import com.ironcladrs.events.queue.*;
import com.ironcladrs.events.sync.*;
import com.ironcladrs.events.ui.IroncladPanel;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.*;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.*;
import net.runelite.client.ui.*;
import net.runelite.client.util.ImageUtil;

/** Lifecycle owns feature subscriptions and the single worker. Game reads stay on ClientThread. */
@Slf4j
@PluginDescriptor(name = "Ironclad Events", description = "Ironclad clan events on ironcladrs.com", tags = {"clan", "events", "ironclad"})
public class IroncladEventsPlugin extends Plugin
{
    @Inject private IroncladEventsConfig config;
    @Inject private ConfigManager configManager;
    @Inject private ClientToolbar toolbar;
    @Inject private ClientThread clientThread;
    @Inject private EventBus eventBus;
    @Inject private ItemManager items;
    @Inject private PlayerGuard guard;
    @Inject private ApiClient api;
    @Inject private SyncService sync;
    @Inject private Outbox outbox;
    @Inject private WorkExecutor work;
    @Inject private Screenshots screenshots;
    @Inject private LootCollector loot;
    @Inject private BankAttestor bank;
    @Inject private PrecheckCapture pictures;
    @Inject private XpCollector xp;
    @Inject private KillCountCollector kc;
    @Inject private ProgressBatcher progress;
    @Inject private CollectionLogReader collection;
    @Inject private NewItemListener newItems;
    @Inject private AccountSnapshot account;
    @Inject private PersonalBestListener pbs;
    @Inject private ClanChatRelay chat;
    @Inject private DeathListener deaths;
    @Inject private ActivityCollector activity;
    @Inject private AccountLinker linker;
    private final List<Object> subscriptions = new ArrayList<>();
    private ScheduledFuture<?> ticker;
    private volatile boolean running;
    private IroncladPanel panel;
    private NavigationButton navigation;
    private boolean navigationVisible;
    private int shownOutbox = -1;
    private long activeGeneration = -1;

    @Override protected void startUp()
    {
        running = true; guard.activate(); work.start();
        register(loot);
        register(bank);
        register(xp);
        register(progress);
        register(collection);
        register(newItems);
        register(account);
        register(pbs);
        register(chat);
        register(deaths);
        register(activity);
        sync.listen(state -> publish(state));
        outbox.listen(message -> {
            int count = outbox.size();
            SwingUtilities.invokeLater(() -> { if (running && panel != null) { panel.status(message, count); } });
        });
        bank.listen(cards -> SwingUtilities.invokeLater(() -> { if (running && panel != null) { panel.bank(cards); } }));
        linker.listen(link -> SwingUtilities.invokeLater(() -> { if (running && panel != null) { panel.link(link); } }));
        SwingUtilities.invokeLater(() -> {
            if (!running) { return; }
            panel = new IroncladPanel(config, items, actions(guard.generation())); panel.link(linker.state());
            navigation = NavigationButton.builder().tooltip("Ironclad Events").priority(5)
                .icon(ImageUtil.loadImageResource(getClass(), "panel_icon.png")).panel(panel).build();
            visibility();
        });
        ticker = work.repeat(() -> clientThread.invoke(() -> { if (running) { pulse(); } }));
        log.info("Ironclad Events started");
    }
    private void register(Object feature) { eventBus.register(feature); subscriptions.add(feature); }
    private void pulse()
    {
        guard.refresh();
        if (activeGeneration != guard.generation()) { resetSession(); }
        Instant now = Instant.now(); sync.tick(now);
        outbox.pump(now); progress.tick(now); account.tick(now); chat.tick(now); activity.tick(now);
        if (shownOutbox != outbox.size())
        { shownOutbox = outbox.size(); int count = shownOutbox; SwingUtilities.invokeLater(() -> { if (running && panel != null) { panel.unsent(count); } }); }
    }
    private void resetSession()
    {
        activeGeneration = guard.generation();
        api.cancelAll(); outbox.clear(); bank.reset(); xp.reset(); kc.reset(); progress.reset();
        collection.reset(); newItems.reset(); account.reset(); pbs.reset(); chat.reset(); deaths.reset(); activity.reset(); screenshots.reset(); sync.reset();
    }
    private void publish(SessionState state)
    {
        int count = outbox.size(); long generation = guard.generation();
        SwingUtilities.invokeLater(() -> { if (running && panel != null) { panel.update(state, count, actions(generation)); } });
    }
    @Override protected void shutDown()
    {
        running = false;
        PlayerGuard.Identity old = guard.identity(); guard.deactivate();
        if (ticker != null) { ticker.cancel(false); ticker = null; }
        linker.cancel(); linker.listen(ignored -> {});
        subscriptions.forEach(eventBus::unregister); subscriptions.clear(); work.stop();
        clientThread.invoke(() -> {
            api.cancelAll(); resetSession();
            if (old != null) { api.request(old, "DELETE", "/session", null, null, null, ignored -> {}); }
        });
        SwingUtilities.invokeLater(() -> {
            if (panel != null) { panel.stop(); }
            if (navigation != null) { toolbar.removeNavigation(navigation); }
            navigationVisible = false; navigation = null; panel = null;
        });
    }
    @Subscribe public void onConfigChanged(ConfigChanged event)
    {
        if (!IroncladEventsConfig.GROUP.equals(event.getGroup()) || !running) { return; }
        SwingUtilities.invokeLater(() -> { if (running) { visibility(); if (panel != null) { panel.render(); } } });
        if (Arrays.asList("showPanel", "chatNotices", "desktopNotices").contains(event.getKey())) { return; }
        // Linking only runs while Connect is on; switching it off stops a link in progress.
        if ("connect".equals(event.getKey()) && !config.connect()) { linker.cancel(); }
        clientThread.invoke(() -> {
            if (!running) { return; }
            PlayerGuard.Identity old = guard.identity(); api.cancelAll(); guard.clear(); guard.refresh(); resetSession();
            if (old != null && !config.connect()) { api.request(old, "DELETE", "/session", null, null, null, ignored -> {}); }
            pulse();
        });
    }
    private void visibility()
    {
        if (navigation == null) { return; }
        if (config.showPanel() && !navigationVisible) { toolbar.addNavigation(navigation); navigationVisible = true; }
        else if (!config.showPanel() && navigationVisible) { toolbar.removeNavigation(navigation); navigationVisible = false; }
    }
    private void dispatch(long generation, Runnable task)
    { clientThread.invoke(() -> { if (running && generation == guard.generation()) { task.run(); } }); }
    private IroncladPanel.Actions actions(long generation)
    {
        return new IroncladPanel.Actions()
        {
            @Override public void clanSettings(boolean enabled)
            { dispatch(generation, () -> configManager.setConfiguration(IroncladEventsConfig.GROUP, "useClanSettings", enabled)); }
            @Override public void sync() { dispatch(generation, () -> sync.manual(Instant.now())); }
            @Override public void link() { if (running) { linker.start(); } }
            @Override public void cancelLink() { linker.cancel(); }
            @Override public void bank(long event, long precheck, boolean automatic) { dispatch(generation, () -> bank.send(event, precheck, automatic)); }
            @Override public void dismissBank(long event, long precheck) { dispatch(generation, () -> bank.dismiss(event, precheck)); }
            @Override public void capture(Consumer<byte[]> preview)
            { dispatch(generation, () -> pictures.capture(bytes -> SwingUtilities.invokeLater(() -> { if (running && generation == guard.generation()) { preview.accept(bytes); } }))); }
            @Override public void picture(long event, long precheck, String value, String note, byte[] bytes)
            { dispatch(generation, () -> pictures.send(event, precheck, value, note, bytes)); }
            @Override public void respond(long id, String response)
            { if (id >= 0 && Arrays.asList("on_it", "done", "cant").contains(response))
              { dispatch(generation, () -> outbox.submit("captain", "/recommendations/" + id + "/response", Json.obj("response", response), null, ignored -> {})); } }
        };
    }
    @Provides IroncladEventsConfig provideConfig(ConfigManager manager) { return manager.getConfig(IroncladEventsConfig.class); }
}
