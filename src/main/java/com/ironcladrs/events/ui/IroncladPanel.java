package com.ironcladrs.events.ui;

import com.google.gson.*;
import com.ironcladrs.events.IroncladEventsConfig;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.sync.SessionState;
import java.awt.*;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.*;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

/** The EDT renders only immutable snapshots; actions return through the client-thread dispatcher. */
public final class IroncladPanel extends PluginPanel
{
    public interface Actions
    {
        void sync(); void bank(long event, long precheck, boolean automatic); void dismissBank(long event, long precheck);
        void capture(Consumer<byte[]> preview); void picture(long event, long precheck, String value, String note, byte[] bytes);
        void respond(long id, String response);
        default void clanSettings(boolean enabled) {}
    }
    private final IroncladEventsConfig config;
    private final ItemManager items;
    private Actions actions;
    private final JPanel content = Ui.column();
    private final javax.swing.Timer timer;
    private StatusFooter footer;
    private long selected = -1;
    private SessionState state = SessionState.empty("Connect in settings to get started.");
    private Map<String, JsonObject> bank = Collections.emptyMap();
    private int unsent;
    private String status = "";
    private boolean showSetup;
    private final JPanel bankContent = Ui.column();
    private final JLabel statusLabel = Ui.text("");
    private final Map<Long, PrecheckList> precheckViews = new HashMap<>();
    private final Map<Long, JsonArray> precheckSchemas = new HashMap<>();
    public IroncladPanel(IroncladEventsConfig config, ItemManager items, Actions actions)
    {
        super(true); this.config = config; this.items = items; this.actions = actions;
        setLayout(new BorderLayout()); setBackground(net.runelite.client.ui.ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8)); add(content, BorderLayout.NORTH);
        getScrollPane().getVerticalScrollBar().setUnitIncrement(16);
        timer = new javax.swing.Timer(1000, event -> { if (footer != null) { footer.update(); } }); timer.start();
    }
    public void stop() { timer.stop(); bank = Collections.emptyMap(); precheckViews.clear(); precheckSchemas.clear(); bankContent.removeAll(); content.removeAll(); }
    public void update(SessionState state, int unsent, Actions actions)
    {
        if (this.state.feature("prechecks") != state.feature("prechecks") || this.state.feature("bankPrechecks") != state.feature("bankPrechecks"))
        { precheckViews.clear(); precheckSchemas.clear(); }
        boolean changed = this.state.linked != state.linked || !this.state.message.equals(state.message)
            || !this.state.sync().equals(state.sync()) || !this.state.hello().equals(state.hello());
        this.state = state; this.unsent = unsent; this.actions = actions;
        if (changed) { render(); } else if (footer != null) { footer.state(state); footer.unsent(unsent); }
    }
    public void bank(Map<String, JsonObject> cards) { bank = cards; renderBank(); }
    public void unsent(int count) { unsent = count; if (footer != null) { footer.unsent(count); } }
    public void status(String text, int unsent)
    { status = text; statusLabel.setText("<html><body style='width:175px'>" + Ui.html(text) + "</body></html>"); unsent(unsent); }
    private void renderBank()
    {
        bankContent.removeAll(); if (state.linked) { for (JsonObject card : bank.values()) { bankContent.add(new BankSendCard(card, items, actions)); } }
        bankContent.revalidate(); bankContent.repaint();
    }
    public void render()
    {
        content.removeAll(); Ui.heading(content, "Ironclad Events");
        content.add(Ui.button("Sync now", () -> this.actions.sync()));
        if (!state.linked) { precheckViews.clear(); precheckSchemas.clear(); content.add(new SetupCard(config, state, actions)); }
        else
        {
            content.add(Ui.button("Connection and uploads", () -> { showSetup = !showSetup; render(); }));
            if (showSetup) { content.add(new SetupCard(config, state, actions)); }
            content.add(Ui.text(state.message)); JsonArray events = state.events();
            if (events.size() == 0)
            { content.add(Ui.text("No live or upcoming rostered events.")); content.add(new ClaimsList(Json.array(state.sync(), "claims"), -1, 5)); Ui.link(content, "Clan events", "https://ironcladrs.com/events"); }
            else
            {
                java.util.List<JsonObject> choices = new ArrayList<>();
                for (JsonElement value : events) { if (value.isJsonObject() && choices.size() < 30) { choices.add(value.getAsJsonObject()); } }
                JsonObject chosen = choices.stream().filter(event -> Json.number(event, "id", -1) == selected).findFirst().orElse(choices.isEmpty() ? new JsonObject() : choices.get(0));
                selected = Json.number(chosen, "id", -1);
                if (choices.size() > 1)
                {
                    JComboBox<String> select = new JComboBox<>(choices.stream().map(event -> Ui.plain(Json.text(event, "name"))).toArray(String[]::new));
                    select.setMaximumSize(new Dimension(195, 28)); select.setSelectedIndex(choices.indexOf(chosen));
                    select.addActionListener(event -> { selected = Json.number(choices.get(select.getSelectedIndex()), "id", -1); render(); }); content.add(select);
                }
                JsonArray owed = Json.array(chosen, "prechecksOwed");
                if (!owed.equals(precheckSchemas.get(selected)))
                { precheckSchemas.put(selected, owed.deepCopy()); precheckViews.put(selected, new PrecheckList(selected, owed, actions, state.feature("prechecks"), state.feature("bankPrechecks"))); }
                content.add(new EventCard(chosen, Json.array(state.sync(), "claims"), items, actions, precheckViews.get(selected), state.feature("captain")));
            }

        }
        renderBank(); content.add(bankContent);
        statusLabel.setText("<html><body style='width:175px'>" + Ui.html(status) + "</body></html>"); content.add(statusLabel);
        footer = new StatusFooter(state, unsent, actions::sync); content.add(footer); content.revalidate(); content.repaint(); revalidate(); repaint();
    }
}
