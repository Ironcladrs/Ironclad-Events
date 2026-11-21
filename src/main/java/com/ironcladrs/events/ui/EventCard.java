package com.ironcladrs.events.ui;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.sync.EventView;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import javax.swing.*;
import net.runelite.client.game.ItemManager;

/**
 * Every event type is rendered from the member's revealed server view, with no local rule engine.
 * The event and the team are the two boxes; everything else is a foldable section that only appears
 * when the server sent something for it, so a quiet event stays short.
 */
public final class EventCard extends JPanel
{
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE MMM d, HH:mm").withZone(ZoneId.systemDefault());

    public EventCard(JsonObject event, JsonArray claims, ItemManager items, IroncladPanel.Actions actions, PrecheckList prechecks, boolean captainAvailable, Set<String> closed)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(LEFT_ALIGNMENT);
        long id = Json.number(event, "id", -1); String key = "e" + id + ":";

        // ── The event ──
        JPanel card = Ui.card(); add(card);
        card.add(Ui.bold(Json.text(event, "name"), Ui.CARD_WIDTH));
        String type = Json.text(event, "typeLabel"); if (!type.isEmpty()) { card.add(Ui.muted(type, Ui.CARD_WIDTH)); }
        Instant now = Instant.now(), start = Json.time(Json.text(event, "startsAt")), end = Json.time(Json.text(event, "endsAt"));
        if (start != null && start.isAfter(now))
        { Ui.gap(card, 4); card.add(Ui.row("Starts in", Ui.span(Duration.between(now, start).toMinutes()), true)); card.add(Ui.muted(WHEN.format(start), Ui.CARD_WIDTH)); }
        else if (end != null)
        {
            Ui.gap(card, 4);
            card.add(end.isAfter(now) ? Ui.row("Time left", Ui.span(Duration.between(now, end).toMinutes()), true) : Ui.row("Event", "Ended", true));
            card.add(Ui.muted("Ends " + WHEN.format(end), Ui.CARD_WIDTH));
        }
        Ui.gap(card, 4); Ui.link(card, "Open event on site", Json.text(event, "url"));
        if (!EventView.supported(event)) { Ui.gap(card, 4); card.add(Ui.text(EventView.message(event), Ui.CARD_WIDTH)); return; }

        // ── Your team ──
        JsonObject team = Json.object(event, "team");
        if (team.size() > 0)
        {
            JPanel box = Ui.card(); Ui.gap(this, 6); add(box);
            box.add(Ui.muted("Your team", Ui.CARD_WIDTH)); box.add(Ui.bold(Json.text(team, "name"), Ui.CARD_WIDTH));
            long rank = Json.number(team, "rank", 0), of = Json.number(team, "of", 0);
            if (rank > 0) { box.add(Ui.row("Rank", rank + " of " + of, true)); }
            String score = Json.text(team, "scoreText"); if (!score.isEmpty()) { box.add(Ui.row("Score", score, false)); }
        }

        // ── What to do now: the team's strategy page ──
        // Only the captain's plan goes here (user, 2026-11-20): their message, priority tasks with progress,
        // and recommendations for you or the whole team. Solo events have no team plan, so no section.
        JsonObject priority = Json.object(event, "priority"); JsonArray steps = Json.array(event, "nextSteps"), recommendations = Json.array(event, "recommendations");
        boolean planned = !Json.text(priority, "text").isEmpty() || steps.size() > 0 || recommendations.size() > 0;
        if (planned || team.size() > 0)
        {
            JPanel body = Ui.section(this, key + "now", "What to do now", closed);
            if (!planned)
            {
                body.add(Ui.muted("Your captain hasn't set a plan yet. It shows here as soon as they save one on the team page.", Ui.WIDTH));
                for (JsonElement value : Json.array(event, "links"))
                { if (value.isJsonObject() && "My team".equals(Json.text(value.getAsJsonObject(), "label"))) { Ui.link(body, "Open team page", Json.text(value.getAsJsonObject(), "url")); } }
            }
            String message = Json.text(priority, "text");
            if (!message.isEmpty()) { body.add(Ui.muted("From your captain", Ui.WIDTH)); body.add(Ui.text(message)); Ui.gap(body, 4); }
            int count = 0;
            for (JsonElement value : steps)
            {
                if (++count > 3) { break; } if (!value.isJsonObject()) { continue; } JsonObject row = value.getAsJsonObject();
                boolean done = Json.flag(row, "done");
                JLabel line = Ui.text((done ? "Done: " : count + ". ") + Json.text(row, "text")); if (done) { line.setForeground(Ui.MUTED); }
                body.add(line);
            }
            int shown = 0;
            for (JsonElement value : recommendations)
            {
                if (++shown > 50) { break; } if (!value.isJsonObject()) { continue; } JsonObject row = value.getAsJsonObject(); long recommendation = Json.number(row, "id", -1);
                Ui.gap(body, 6); body.add(Ui.muted("Captain's ask", Ui.WIDTH)); body.add(Ui.text(Json.text(row, "text")));
                String response = Json.text(row, "response"); if (!response.isEmpty()) { body.add(Ui.muted("Your answer: " + response.replace('_', ' '), Ui.WIDTH)); }
                if (captainAvailable)
                {
                    body.add(Ui.buttons(Ui.button("On it", () -> actions.respond(recommendation, "on_it")),
                        Ui.button("Done", () -> actions.respond(recommendation, "done")), Ui.button("Can't", () -> actions.respond(recommendation, "cant"))));
                }
                else { body.add(Ui.muted("Answer this on the site for now.", Ui.WIDTH)); }
            }
        }

        // ── Prechecks ──
        int owed = Json.array(event, "prechecksOwed").size();
        if (owed > 0 && prechecks != null) { Ui.section(this, key + "prechecks", "Prechecks owed (" + owed + ")", closed).add(prechecks); }

        // ── Standings ──
        JsonArray standings = Json.array(event, "standings");
        if (standings.size() > 0)
        {
            JPanel body = Ui.section(this, key + "standings", "Standings", closed); int rank = 0;
            for (JsonElement value : standings)
            {
                if (++rank > 5) { break; } if (!value.isJsonObject()) { continue; } JsonObject row = value.getAsJsonObject();
                boolean mine = Json.flag(row, "mine");
                body.add(Ui.row(rank + ". " + Json.text(row, "name") + (mine ? " (you)" : ""), Json.text(row, "scoreText"), mine));
            }
        }

        // ── Tasks ──
        JsonArray tasks = Json.array(event, "tasks");
        if (tasks.size() > 0)
        {
            int done = 0; for (JsonElement task : tasks) { if (task.isJsonObject() && Json.flag(task.getAsJsonObject(), "done")) { done++; } }
            Ui.section(this, key + "tasks", "Tasks (" + done + " of " + tasks.size() + " done)", closed).add(new TaskList(tasks, items));
        }

        // ── My claims ──
        Ui.section(this, key + "claims", "My claims", closed).add(new ClaimsList(claims, id, 25));

        // ── Live progress ──
        JPanel progress = Ui.section(this, key + "progress", "Live progress", closed); int rows = 0;
        for (JsonElement value : Json.array(event, "progress"))
        {
            if (++rows > 100) { break; } if (!value.isJsonObject()) { continue; } JsonObject row = value.getAsJsonObject();
            progress.add(Ui.row(Json.text(row, "label"), Json.text(row, "confirmedText"), false));
            String provisional = Json.text(row, "provisionalText");
            if (!provisional.isEmpty()) { progress.add(Ui.muted("+ " + provisional + " waiting for Wise Old Man", Ui.WIDTH)); }
        }
        if (rows == 0) { progress.add(Ui.muted("Nothing yet. Gains count once Wise Old Man confirms them.", Ui.WIDTH)); }

        // ── More links ──
        JsonArray links = Json.array(event, "links");
        if (links.size() > 0)
        {
            JPanel body = Ui.section(this, key + "links", "Links", closed); int count = 0;
            for (JsonElement value : links)
            { if (++count > 8) { break; } if (value.isJsonObject()) { JsonObject link = value.getAsJsonObject(); Ui.link(body, Json.text(link, "label"), Json.text(link, "url")); } }
        }
    }
}
