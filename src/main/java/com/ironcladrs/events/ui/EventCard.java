package com.ironcladrs.events.ui;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.sync.EventView;
import java.time.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import net.runelite.client.game.ItemManager;

/** Every event type is rendered from the member's revealed server view, with no local rule engine. */
public final class EventCard extends JPanel
{
    public EventCard(JsonObject event, JsonArray claims, ItemManager items, IroncladPanel.Actions actions, PrecheckList prechecks, boolean captainAvailable)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false);
        long id = Json.number(event, "id", -1); Ui.heading(this, Json.text(event, "name")); add(Ui.text(Json.text(event, "typeLabel")));
        Instant start = Json.time(Json.text(event, "startsAt")), end = Json.time(Json.text(event, "endsAt"));
        DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM d, HH:mm z").withZone(ZoneId.systemDefault());
        if (start != null && start.isAfter(Instant.now())) { add(Ui.text("Starts " + format.format(start))); }
        if (end != null)
        {
            long minutes = Math.max(0, Duration.between(Instant.now(), end).toMinutes());
            add(Ui.text("Time left: " + minutes / 60 + "h " + minutes % 60 + "m")); add(Ui.text("Ends " + format.format(end)));
        }
        if (!EventView.supported(event))
        {
            add(Ui.text(EventView.message(event))); Ui.link(this, "Open event", Json.text(event, "url")); return;
        }
        JsonObject team = Json.object(event, "team");
        if (team.size() > 0) { add(Ui.text(Json.text(team, "name") + " · " + Json.number(team, "rank", 0) + " of " + Json.number(team, "of", 0))); add(Ui.text(Json.text(team, "scoreText"))); }
        Ui.link(this, "Open event", Json.text(event, "url")); int links = 0;
        for (JsonElement value : Json.array(event, "links"))
        { if (++links > 8) { break; } if (value.isJsonObject()) { JsonObject link = value.getAsJsonObject(); Ui.link(this, Json.text(link, "label"), Json.text(link, "url")); } }
        Ui.heading(this, "Standings"); int rank = 0;
        for (JsonElement value : Json.array(event, "standings"))
        { if (++rank > 5) { break; } if (value.isJsonObject()) { JsonObject row = value.getAsJsonObject(); add(Ui.text(Json.text(row, "name") + (Json.flag(row, "mine") ? " (you)" : "") + " · " + Json.text(row, "scoreText"))); } }
        Ui.heading(this, "Now"); int step = 0;
        for (JsonElement value : Json.array(event, "nextSteps"))
        { if (++step > 3) { break; } if (value.isJsonObject()) { JsonObject row = value.getAsJsonObject(); add(Ui.text(Json.text(row, "text"))); JLabel reason = Ui.text(Json.text(row, "reason")); reason.setForeground(java.awt.Color.GRAY); add(reason); } }
        Ui.heading(this, "Captain"); JsonObject priority = Json.object(event, "priority"); add(Ui.text(Json.text(priority, "text")));
        for (JsonElement target : Json.array(priority, "targets")) { if (target.isJsonPrimitive()) { add(Ui.text(target.getAsString())); } }
        int recommendations = 0;
        for (JsonElement value : Json.array(event, "recommendations"))
        {
            if (++recommendations > 50) { break; } if (!value.isJsonObject()) { continue; } JsonObject row = value.getAsJsonObject(); long recommendation = Json.number(row, "id", -1);
            add(Ui.text(Json.text(row, "text"))); add(Ui.text(Json.text(row, "response")));
            if (captainAvailable)
            { add(Ui.button("On it", () -> actions.respond(recommendation, "on_it"))); add(Ui.button("Done", () -> actions.respond(recommendation, "done"))); add(Ui.button("Can't", () -> actions.respond(recommendation, "cant"))); }
            else { add(Ui.text("Plugin responses are not available yet. Use the site.")); }
        }
        Ui.heading(this, "Prechecks owed"); add(prechecks);
        Ui.heading(this, "Tasks"); add(new TaskList(Json.array(event, "tasks"), items));
        Ui.heading(this, "My claims"); add(new ClaimsList(claims, id, 25));
        Ui.heading(this, "Live progress");
        int progress = 0;
        for (JsonElement value : Json.array(event, "progress"))
        { if (++progress > 100) { break; } if (value.isJsonObject()) { JsonObject row = value.getAsJsonObject(); add(Ui.text(Json.text(row, "label") + " · confirmed " + Json.text(row, "confirmedText"))); String provisional = Json.text(row, "provisionalText"); if (!provisional.isEmpty()) { add(Ui.text("⋯ " + provisional + " · not confirmed yet")); } } }
        if (progress == 0) { add(Ui.text("Live reports only count after Wise Old Man confirms them.")); }
    }
}
