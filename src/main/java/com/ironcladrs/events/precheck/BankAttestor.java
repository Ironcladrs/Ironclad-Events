package com.ironcladrs.events.precheck;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.queue.*;
import com.ironcladrs.events.sync.SyncService;
import java.time.Instant;
import java.util.*;
import java.util.function.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;

/** Only an open bank can create evidence. The full containers never enter an upload DTO. */
@Singleton
public class BankAttestor
{
    @Inject private Client client;
    @Inject private ItemManager items;
    @Inject private SyncService sync;
    @Inject private Consent consent;
    @Inject private Outbox outbox;
    private final Map<String, JsonObject> cards = new LinkedHashMap<>();
    private final Set<Long> automatic = new HashSet<>();
    private final Set<String> submitted = new HashSet<>();
    private final Set<String> dismissed = new HashSet<>();
    private Consumer<Map<String, JsonObject>> listener = ignored -> {};
    @Subscribe public void onWidgetLoaded(WidgetLoaded event)
    { if (event.getGroupId() == InterfaceID.BANKMAIN) { dismissed.clear(); } }
    public void listen(Consumer<Map<String, JsonObject>> listener) { this.listener = listener; }
    @Subscribe public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (!consent.allows("bankPrechecks")) { return; }
        if (event.getContainerId() != InventoryID.BANK && cards.isEmpty()) { return; }
        if (event.getContainerId() != InventoryID.BANK && event.getContainerId() != InventoryID.INV && event.getContainerId() != InventoryID.WORN) { return; }
        ItemContainer bank = client.getItemContainer(InventoryID.BANK), inventory = client.getItemContainer(InventoryID.INV), worn = client.getItemContainer(InventoryID.WORN);
        if (bank == null || inventory == null || worn == null) { return; }
        cards.clear();
        for (JsonElement eventValue : sync.state().events())
        {
            if (!eventValue.isJsonObject()) { continue; }
            JsonObject eventData = eventValue.getAsJsonObject(); long eventId = Json.number(eventData, "id", -1);
            Instant end = Json.time(Json.text(eventData, "endsAt"));
            if (eventId < 0 || end == null || !Instant.now().isBefore(end)) { continue; }
            for (JsonElement value : Json.array(eventData, "prechecksOwed"))
            {
                if (!value.isJsonObject()) { continue; }
                JsonObject precheck = value.getAsJsonObject();
                if (!"bank".equals(Json.text(precheck, "kind"))) { continue; }
                long precheckId = Json.number(precheck, "id", -1); String key = key(eventId, precheckId);
                if (precheckId < 0 || submitted.contains(key) || dismissed.contains(key)) { continue; }
                JsonArray asked = Json.array(Json.object(precheck, "ask"), "itemIds");
                if (asked.size() == 0 || asked.size() > 100) { continue; }
                JsonObject counts = count(asked, Arrays.asList(bank.getItems(), inventory.getItems(), worn.getItems()), items::canonicalize);
                JsonObject card = Json.obj("eventId", eventId, "precheckId", precheckId, "label", Json.text(precheck, "label"), "counts", counts, "at", Instant.now());
                JsonObject names = new JsonObject();
                counts.keySet().forEach(id -> names.addProperty(id, items.getItemComposition(Integer.parseInt(id)).getMembersName()));
                card.add("names", names); cards.put(key, card);
            }
        }
        for (JsonObject card : new ArrayList<>(cards.values()))
        { if (automatic.contains(Json.number(card, "eventId", -1))) { send(Json.number(card, "eventId", -1), Json.number(card, "precheckId", -1), false); } }
        publish();
    }
    public static JsonObject count(JsonArray asked, List<Item[]> containers, IntUnaryOperator canonicalize)
    {
        Map<Integer, Integer> askedCanonical = new LinkedHashMap<>(); JsonObject result = new JsonObject();
        for (JsonElement value : asked)
        { int id = value.getAsInt(); if (id > 0) { askedCanonical.put(id, canonicalize.applyAsInt(id)); result.addProperty(Integer.toString(id), 0L); } }
        for (Item[] container : containers)
        { for (Item item : container)
        { if (item == null || item.getId() < 0 || item.getQuantity() <= 0) { continue; }
          int id = canonicalize.applyAsInt(item.getId());
          askedCanonical.forEach((askedId, canonical) -> { if (id == canonical) { String key = askedId.toString(); result.addProperty(key, result.get(key).getAsLong() + item.getQuantity()); } });
        } }
        return result;
    }
    public void send(long eventId, long precheckId, boolean automatically)
    {
        String key = key(eventId, precheckId); JsonObject card = cards.get(key);
        if (card == null || !consent.allows("bankPrechecks")) { return; }
        // Expired cards require a fresh bank event; do not attest against stale inventory.
        Instant at = Json.time(Json.text(card, "at"));
        if (at == null || at.plusSeconds(120).isBefore(Instant.now())) { cards.remove(key); publish(); return; }
        JsonArray sources = new JsonArray(); sources.add("bank"); sources.add("inventory"); sources.add("equipment");
        JsonObject payload = card.deepCopy(); payload.remove("label"); payload.remove("names"); payload.add("sources", sources);
        if (outbox.submit("bankPrechecks", "/precheck/attest", payload, null, result -> {
            if (!result.success()) { submitted.remove(key); }
        }))
        { submitted.add(key); cards.remove(key); if (automatically) { automatic.add(eventId); } publish(); }
    }
    public void dismiss(long eventId, long precheckId)
    { String key = key(eventId, precheckId); dismissed.add(key); cards.remove(key); publish(); }
    public void reset() { cards.clear(); automatic.clear(); submitted.clear(); dismissed.clear(); publish(); }
    private static String key(long eventId, long precheckId) { return eventId + ":" + precheckId; }
    private void publish()
    {
        Map<String, JsonObject> copy = new LinkedHashMap<>(); cards.forEach((key, value) -> copy.put(key, value.deepCopy()));
        listener.accept(Collections.unmodifiableMap(copy));
    }
}
