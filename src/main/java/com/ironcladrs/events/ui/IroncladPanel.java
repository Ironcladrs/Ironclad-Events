package com.ironcladrs.events.ui;

import com.google.gson.*;
import com.ironcladrs.events.IroncladEventsConfig;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.sync.AccountLinker;
import com.ironcladrs.events.sync.SessionState;
import java.awt.*;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.*;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

/** The EDT renders only immutable snapshots; actions return through the client-thread dispatcher. */
public final class IroncladPanel extends PluginPanel
{
    public interface Actions
    {
        void sync(); void bank(long event, long precheck, boolean automatic); void dismissBank(long event, long precheck);
        void capture(Consumer<byte[]> preview); void picture(long event, long precheck, String value, String note, byte[] bytes);
        void respond(long id, String response);
        default void clanSettings(boolean enabled) {}
        /** Switch on these upload keys plus the clan's loot filters; the plugin re-checks every key. */
        default void clanDefaults(java.util.List<String> keys) {}
        default void link() {}
        default void cancelLink() {}
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
    /** Sections the member folded shut; kept across syncs so a re-render never reopens them. */
    private final Set<String> closed = new HashSet<>();
    private final Icon logo = loadLogo();
    private AccountLinker.State link = AccountLinker.State.IDLE_STATE;
    /** Which tab is open: the event, or the member's own claims. */
    private String tab = "event";
    public IroncladPanel(IroncladEventsConfig config, ItemManager items, Actions actions)
    {
        super(true); this.config = config; this.items = items; this.actions = actions;
        setLayout(new BorderLayout()); setBackground(net.runelite.client.ui.ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8)); add(content, BorderLayout.NORTH);
        getScrollPane().getVerticalScrollBar().setUnitIncrement(16);
        timer = new javax.swing.Timer(1000, event -> { if (footer != null) { footer.update(); } }); timer.start();
    }
    public void stop() { timer.stop(); closed.clear(); bank = Collections.emptyMap(); precheckViews.clear(); precheckSchemas.clear(); bankContent.removeAll(); content.removeAll(); }
    public void update(SessionState state, int unsent, Actions actions)
    {
        if (this.state.feature("prechecks") != state.feature("prechecks") || this.state.feature("bankPrechecks") != state.feature("bankPrechecks"))
        { precheckViews.clear(); precheckSchemas.clear(); }
        boolean changed = this.state.linked != state.linked || !this.state.message.equals(state.message)
            || !this.state.sync().equals(state.sync()) || !this.state.hello().equals(state.hello());
        this.state = state; this.unsent = unsent; this.actions = actions;
        if (changed) { render(); } else if (footer != null) { footer.state(state); footer.unsent(unsent); }
    }
    /** Linking progress comes from the linker; a change redraws the setup view. */
    public void link(AccountLinker.State state)
    {
        if (state == null || state == link) { return; }
        boolean fresh = state.phase == AccountLinker.State.Phase.WAITING && !(link.phase == AccountLinker.State.Phase.WAITING && link.code.equals(state.code));
        link = state; render();
        // Open the approve page once per new code, and only ever on the clan site.
        if (fresh && Ui.allowedLink(state.url)) { net.runelite.client.util.LinkBrowser.browse(state.url); }
    }
    public void bank(Map<String, JsonObject> cards) { bank = cards; renderBank(); }
    public void unsent(int count) { unsent = count; if (footer != null) { footer.unsent(count); } }
    public void status(String text, int unsent)
    { status = text; statusLabel.setText("<html><body style='width:" + Ui.css(Ui.WIDTH) + "px'>" + Ui.html(text) + "</body></html>"); unsent(unsent); }
    private void renderBank()
    {
        bankContent.removeAll(); if (state.linked) { for (JsonObject card : bank.values()) { Ui.gap(bankContent, 6); bankContent.add(new BankSendCard(card, items, actions)); } }
        bankContent.revalidate(); bankContent.repaint();
    }
    private static Icon loadLogo()
    {
        try { return new ImageIcon(ImageUtil.loadImageResource(IroncladPanel.class, "/com/ironcladrs/events/logo.png")); }
        catch (RuntimeException ex) { return null; }
    }
    private JPanel header()
    {
        JPanel header = new JPanel(new BorderLayout(8, 0)); header.setOpaque(false);
        if (logo != null) { header.add(new JLabel(logo), BorderLayout.WEST); }
        JPanel words = Ui.column(); words.setOpaque(false);
        JLabel title = new JLabel("Ironclad Events"); title.setFont(FontManager.getRunescapeBoldFont()); title.setForeground(Color.WHITE);
        words.add(Box.createVerticalGlue()); words.add(title);
        String clan = state.clanName();
        words.add(Ui.line(state.linked ? Ui.pill("Connected", Ui.GOOD) : Ui.pill("Not connected", Ui.MUTED), clan.isEmpty() ? null : Ui.pill(clan, Ui.GOLD)));
        words.add(Box.createVerticalGlue());
        header.add(words, BorderLayout.CENTER);
        // Sync is reachable at the top as well as in the footer of a long list.
        JPanel right = new JPanel(new GridBagLayout()); right.setOpaque(false); JButton sync = Ui.button("Sync", () -> this.actions.sync()); sync.setToolTipText("Sync now"); right.add(sync);
        header.add(right, BorderLayout.EAST);
        return Ui.fit(header);
    }
    /** Event | My claims, as one strip of buttons; the open tab is lighter and says so to screen readers. */
    private JPanel tabs()
    {
        JPanel strip = new JPanel(new GridLayout(1, 2, 2, 0)); strip.setOpaque(false);
        String[][] all = {{"event", "Event"}, {"me", "My claims"}};
        for (String[] t : all)
        {
            boolean open = t[0].equals(tab);
            JButton button = new JButton(t[1]); button.setFocusPainted(false); button.setContentAreaFilled(false); button.setOpaque(true);
            button.setFont(open ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeSmallFont());
            button.setBackground(open ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR); button.setForeground(open ? Color.WHITE : Ui.MUTED);
            button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, open ? Ui.GOLD : ColorScheme.DARKER_GRAY_COLOR), BorderFactory.createEmptyBorder(6, 2, 4, 2)));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); button.getAccessibleContext().setAccessibleName(t[1] + (open ? ", open" : ""));
            button.addActionListener(e -> { tab = t[0]; render(); getScrollPane().getVerticalScrollBar().setValue(0); });
            strip.add(button);
        }
        return Ui.fit(strip);
    }
    /** For the development preview only: open a tab before drawing. */
    public void showTab(String name) { tab = name; render(); }
    public void render()
    {
        content.removeAll(); content.add(header()); Ui.gap(content, 8); content.add(tabs()); Ui.gap(content, 6);
        if (!state.linked) { precheckViews.clear(); precheckSchemas.clear(); content.add(new SetupCard(config, state, link, actions)); }
        else if (tab.equals("me")) { renderMine(); }
        else { renderEvent(); }
        statusLabel.setText("<html><body style='width:" + Ui.css(Ui.WIDTH) + "px'>" + Ui.html(status) + "</body></html>"); content.add(statusLabel);
        footer = new StatusFooter(state, unsent, actions::sync); content.add(footer); content.revalidate(); content.repaint(); revalidate(); repaint();
    }
    private void renderMine()
    {
        if (!state.message.isEmpty() && !state.message.equals("Synced")) { content.add(Ui.muted(state.message, Ui.WIDTH)); }
        JPanel claims = Ui.section(content, "claims:any", "My recent claims", null, closed); claims.add(new ClaimsList(Json.array(state.sync(), "claims"), -1, 25));
        Ui.gap(content, 6); content.add(new ClanSettingsCard(config, state, actions));
    }
    private void renderEvent()
    {
        if (!state.message.isEmpty() && !state.message.equals("Synced")) { content.add(Ui.muted(state.message, Ui.WIDTH)); }
        JsonArray events = state.events();
        if (events.size() == 0)
        {
            JPanel empty = Ui.card(); content.add(empty);
            empty.add(Ui.bold("No events right now", Ui.CARD_WIDTH));
            empty.add(Ui.muted("You are not on the roster of a live or upcoming event. Sign up on the site, then sync.", Ui.CARD_WIDTH));
            Ui.gap(empty, 4); Ui.link(empty, "Clan events", "https://ironcladrs.com/events");
        }
        else
        {
            java.util.List<JsonObject> choices = new ArrayList<>();
            for (JsonElement value : events) { if (value.isJsonObject() && choices.size() < 30) { choices.add(value.getAsJsonObject()); } }
            JsonObject chosen = choices.stream().filter(event -> Json.number(event, "id", -1) == selected).findFirst().orElse(choices.isEmpty() ? new JsonObject() : choices.get(0));
            selected = Json.number(chosen, "id", -1);
            if (choices.size() > 1)
            {
                JComboBox<String> select = new JComboBox<>(choices.stream().map(event -> Ui.plain(Json.text(event, "name"))).toArray(String[]::new));
                select.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); select.setAlignmentX(Component.LEFT_ALIGNMENT); select.setSelectedIndex(choices.indexOf(chosen));
                select.setToolTipText("Your events (" + choices.size() + ")"); select.getAccessibleContext().setAccessibleName("Choose an event");
                select.addActionListener(event -> { selected = Json.number(choices.get(select.getSelectedIndex()), "id", -1); render(); }); content.add(select); Ui.gap(content, 6);
            }
            JsonArray owed = Json.array(chosen, "prechecksOwed");
            if (!owed.equals(precheckSchemas.get(selected)))
            { precheckSchemas.put(selected, owed.deepCopy()); precheckViews.put(selected, new PrecheckList(selected, owed, actions, state.feature("prechecks"), state.feature("bankPrechecks"))); }
            content.add(new EventCard(chosen, Json.array(state.sync(), "claims"), items, actions, precheckViews.get(selected), state.feature("captain"), closed));
        }
        renderBank(); content.add(bankContent);
    }
}
