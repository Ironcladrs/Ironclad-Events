package com.ironcladrs.events.ui;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.sync.EventView;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.awt.*;
import java.util.Set;
import javax.swing.*;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.game.ItemManager;

/**
 * Every event type is rendered from the member's revealed server view, with no local rule engine.
 * The event and the team are the two boxes (with their countdown and score bars); everything else is a
 * foldable section that only appears when the server sent something for it, so a quiet event stays short.
 * Redesigned 2026-11-27 (user: "the data presented in the plugin should be presented better").
 */
public final class EventCard extends JPanel
{
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE MMM d, HH:mm").withZone(ZoneId.systemDefault());
    private static final Color[] MEDALS = {new Color(230, 172, 30), new Color(190, 196, 204), new Color(196, 128, 72)};

    public EventCard(JsonObject event, JsonArray claims, ItemManager items, IroncladPanel.Actions actions, PrecheckList prechecks, boolean captainAvailable, Set<String> closed)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(LEFT_ALIGNMENT);
        long id = Json.number(event, "id", -1); String key = "e" + id + ":";

        // ── The event: name, tags, countdown and how far through it is ──
        JPanel card = Ui.card(); add(card);
        card.add(Ui.bold(Json.text(event, "name"), Ui.CARD_WIDTH));
        Instant now = Instant.now(), start = Json.time(Json.text(event, "startsAt")), end = Json.time(Json.text(event, "endsAt"));
        boolean live = "live".equals(Json.text(event, "state")) && (start == null || !start.isAfter(now)) && (end == null || end.isAfter(now));
        String type = Json.text(event, "typeLabel");
        card.add(Ui.line(type.isEmpty() ? null : Ui.pill(type, Ui.MUTED), live ? Ui.pill("Live", Ui.GOOD) : end != null && !end.isAfter(now) ? Ui.pill("Ended", Ui.BAD) : Ui.pill("Upcoming", Ui.ACCENT)));
        Ui.gap(card, 6);
        if (start != null && start.isAfter(now))
        {
            card.add(big("Starts in " + Ui.span(Duration.between(now, start).toMinutes())));
            card.add(Ui.muted(WHEN.format(start), Ui.CARD_WIDTH));
        }
        else if (end != null)
        {
            card.add(big(end.isAfter(now) ? Ui.span(Duration.between(now, end).toMinutes()) + " left" : "Finished"));
            if (start != null && end.isAfter(start))
            { Ui.gap(card, 3); card.add(Ui.bar((double) Duration.between(start, now).toMinutes() / Duration.between(start, end).toMinutes(), Ui.GOLD)); Ui.gap(card, 3); }
            card.add(Ui.muted("Ends " + WHEN.format(end), Ui.CARD_WIDTH));
        }
        Ui.gap(card, 6); Ui.link(card, "Open event on site", Json.text(event, "url"));
        if (!EventView.supported(event)) { Ui.gap(card, 4); card.add(Ui.text(EventView.message(event), Ui.CARD_WIDTH)); return; }

        // ── Prechecks: something to do now, so straight under the event, above team and standings ──
        int owed = Json.array(event, "prechecksOwed").size();
        if (owed > 0 && prechecks != null) { Ui.section(this, key + "prechecks", "Prechecks owed", Integer.toString(owed), closed).add(prechecks); }

        // ── Domination: territories one step from taking or losing (server: pluginAreas.js) ──
        JsonArray closeCalls = Json.array(event, "closeCalls");
        if (closeCalls.size() > 0)
        {
            JPanel body = Ui.section(this, key + "close", "Close calls", Integer.toString(closeCalls.size()), closed);
            for (JsonElement value : closeCalls)
            {
                if (!value.isJsonObject()) { continue; } JsonObject row = value.getAsJsonObject(); boolean risk = "risk".equals(Json.text(row, "kind"));
                body.add(Ui.line(Ui.pill(risk ? "At risk" : "Within reach", risk ? Ui.BAD : Ui.GOOD), Ui.bold(Json.text(row, "name"), Ui.CARD_WIDTH - 80)));
                body.add(Ui.muted(Json.text(row, "text"), Ui.WIDTH)); Ui.gap(body, 4);
            }
        }

        // ── Battleship Bingo: the enemy fleet, updated as your team sinks ships and finds buoys ──
        JsonObject fleet = Json.object(event, "fleet");
        if (fleet.size() > 0) { fleet(fleet, key, closed); }

        // ── Your team: rank and score, with a bar when the score is "x of y" ──
        JsonObject team = Json.object(event, "team");
        if (team.size() > 0)
        {
            JPanel box = Ui.card(); Ui.gap(this, 6); add(box);
            long rank = Json.number(team, "rank", 0), of = Json.number(team, "of", 0);
            JPanel top = new JPanel(new BorderLayout(6, 0)); top.setOpaque(false);
            JLabel name = Ui.name(Json.text(team, "name")); name.setFont(FontManager.getRunescapeBoldFont()); name.setForeground(Color.WHITE);
            top.add(name, BorderLayout.CENTER);
            if (rank > 0) { top.add(Ui.pill("#" + rank + " of " + of, rank <= 3 ? MEDALS[(int) rank - 1] : Ui.MUTED), BorderLayout.EAST); }
            box.add(Ui.muted("Your team", Ui.CARD_WIDTH)); box.add(Ui.fit(top));
            String score = Json.text(team, "scoreText");
            if (!score.isEmpty())
            {
                Ui.gap(box, 4); box.add(Ui.row("Score", score, false));
                double f = Ui.fraction(score); if (!Double.isNaN(f)) { Ui.gap(box, 2); box.add(Ui.bar(f, Ui.GOOD)); }
            }
        }

        // ── What to do now: the team's strategy page ──
        // Only the captain's plan goes here (user, 2026-11-20): their message, priority tasks with progress,
        // and recommendations for you or the whole team. Solo events have no team plan, so no section.
        JsonObject priority = Json.object(event, "priority"); JsonArray steps = Json.array(event, "nextSteps"), recommendations = Json.array(event, "recommendations");
        boolean planned = !Json.text(priority, "text").isEmpty() || steps.size() > 0 || recommendations.size() > 0;
        if (planned || team.size() > 0)
        {
            JPanel body = Ui.section(this, key + "now", "What to do now", recommendations.size() > 0 ? recommendations.size() + " ask" + (recommendations.size() == 1 ? "" : "s") : null, closed);
            if (!planned)
            {
                body.add(Ui.muted("Your captain hasn't set a plan yet. It shows here as soon as they save one on the team page.", Ui.WIDTH));
                for (JsonElement value : Json.array(event, "links"))
                { if (value.isJsonObject() && "My team".equals(Json.text(value.getAsJsonObject(), "label"))) { Ui.link(body, "Open team page", Json.text(value.getAsJsonObject(), "url")); } }
            }
            String message = Json.text(priority, "text");
            if (!message.isEmpty())
            {
                JPanel quote = Ui.column(); quote.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                quote.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, Ui.GOLD), BorderFactory.createEmptyBorder(5, 7, 5, 6)));
                JLabel from = Ui.muted("From your captain", Ui.BESIDE_ICON + 20); from.setForeground(Ui.GOLD); quote.add(from); quote.add(Ui.text(message, Ui.BESIDE_ICON + 20));
                body.add(Ui.fit(quote)); Ui.gap(body, 4);
            }
            int count = 0;
            for (JsonElement value : steps)
            {
                if (++count > 3) { break; } if (!value.isJsonObject()) { continue; } JsonObject row = value.getAsJsonObject();
                boolean done = Json.flag(row, "done");
                JLabel line = Ui.text((done ? "Done: " : "") + Json.text(row, "text"), Ui.WIDTH - 18); line.setIcon(new Ui.StateDot(done)); line.setIconTextGap(6);
                if (done) { line.setForeground(Ui.MUTED); }
                body.add(line);
            }
            int shown = 0;
            for (JsonElement value : recommendations)
            {
                if (++shown > 50) { break; } if (!value.isJsonObject()) { continue; } JsonObject row = value.getAsJsonObject(); long recommendation = Json.number(row, "id", -1);
                JPanel ask = Ui.card(); Ui.gap(body, 6); body.add(ask);
                JLabel title = Ui.muted("Captain's ask", Ui.CARD_WIDTH); title.setForeground(Ui.GOLD); ask.add(title); ask.add(Ui.text(Json.text(row, "text"), Ui.CARD_WIDTH));
                String response = Json.text(row, "response"); if (!response.isEmpty()) { ask.add(Ui.muted("Your answer: " + response.replace('_', ' '), Ui.CARD_WIDTH)); }
                Ui.gap(ask, 4);
                if (captainAvailable)
                {
                    ask.add(Ui.buttons(Ui.button("On it", () -> actions.respond(recommendation, "on_it")),
                        Ui.button("Done", () -> actions.respond(recommendation, "done")), Ui.button("Can't", () -> actions.respond(recommendation, "cant"))));
                }
                else { ask.add(Ui.muted("Answer this on the site for now.", Ui.CARD_WIDTH)); }
            }
        }

        // ── Domination: who holds each region and its kingdom bonus ──
        JsonArray areas = Json.array(event, "areas");
        if (areas.size() > 0)
        {
            int ours = 0; for (JsonElement value : areas) { if (value.isJsonObject() && "yours".equals(Json.text(value.getAsJsonObject(), "status"))) { ours++; } }
            JPanel body = Ui.section(this, key + "areas", "Regions", ours + " of " + areas.size() + " yours", closed);
            for (JsonElement value : areas)
            {
                if (!value.isJsonObject()) { continue; } JsonObject row = value.getAsJsonObject(); String status = Json.text(row, "status");
                body.add(Ui.line(Ui.pill("yours".equals(status) ? "Yours" : "theirs".equals(status) ? "Theirs" : "Open", "yours".equals(status) ? Ui.GOOD : "theirs".equals(status) ? Ui.BAD : Ui.MUTED),
                    Ui.bold(Json.text(row, "name"), Ui.CARD_WIDTH - 60)));
                body.add(Ui.muted(Json.text(row, "text"), Ui.WIDTH)); Ui.gap(body, 4);
            }
        }

        // ── Standings: medal colours for the top three, your team highlighted ──
        JsonArray standings = Json.array(event, "standings");
        if (standings.size() > 0)
        {
            JPanel body = Ui.section(this, key + "standings", "Standings", null, closed); int rank = 0;
            for (JsonElement value : standings)
            {
                if (++rank > 5) { break; } if (!value.isJsonObject()) { continue; } JsonObject row = value.getAsJsonObject();
                body.add(standing(rank, Json.text(row, "name"), Json.text(row, "scoreText"), Json.flag(row, "mine")));
            }
        }

        // ── Tasks ──
        JsonArray tasks = Json.array(event, "tasks");
        if (tasks.size() > 0)
        {
            int done = 0; for (JsonElement task : tasks) { if (task.isJsonObject() && Json.flag(task.getAsJsonObject(), "done")) { done++; } }
            JPanel body = Ui.section(this, key + "tasks", "Tasks", done + " of " + tasks.size() + " done", closed);
            body.add(Ui.bar((double) done / tasks.size(), Ui.GOOD)); Ui.gap(body, 6);
            body.add(new TaskList(tasks, items));
        }

        // ── Live progress (only when there is some) ──
        JsonArray progressRows = Json.array(event, "progress");
        if (progressRows.size() > 0)
        {
            JPanel progress = Ui.section(this, key + "progress", "Live progress", null, closed); int rows = 0;
            for (JsonElement value : progressRows)
            {
                if (++rows > 100) { break; } if (!value.isJsonObject()) { continue; } JsonObject row = value.getAsJsonObject();
                progress.add(Ui.row(Json.text(row, "label"), Json.text(row, "confirmedText"), false));
                String provisional = Json.text(row, "provisionalText");
                if (!provisional.isEmpty()) { progress.add(Ui.muted("+ " + provisional + " waiting for Wise Old Man", Ui.WIDTH)); }
            }
        }

        // ── Links: two to a row ──
        JsonArray links = Json.array(event, "links");
        if (links.size() > 0)
        {
            JPanel body = Ui.section(this, key + "links", "On the site", null, closed);
            JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4)); grid.setOpaque(false); int count = 0;
            for (JsonElement value : links)
            {
                if (++count > 8 || !value.isJsonObject()) { continue; } JsonObject link = value.getAsJsonObject(); String url = Json.text(link, "url");
                if (Ui.allowedLink(url)) { grid.add(Ui.button(Json.text(link, "label"), () -> net.runelite.client.util.LinkBrowser.browse(url))); }
            }
            body.add(Ui.fit(grid));
        }
    }

    private static JLabel big(String text)
    { JLabel label = new JLabel(Ui.plain(text)); label.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f)); label.setForeground(Color.WHITE); label.setAlignmentX(LEFT_ALIGNMENT); return label; }

    /** One standings line: a medal-coloured rank, the team, its score; your team on a lighter band. */
    private static JPanel standing(int rank, String name, String score, boolean mine)
    {
        JPanel row = new JPanel(new BorderLayout(6, 0)); row.setOpaque(mine); row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JLabel place = new JLabel(Integer.toString(rank)); place.setFont(FontManager.getRunescapeBoldFont()); place.setPreferredSize(new Dimension(14, 14));
        place.setForeground(rank <= 3 ? MEDALS[rank - 1] : Ui.MUTED);
        JLabel team = Ui.name(name + (mine ? " (you)" : "")); team.setForeground(mine ? Color.WHITE : Ui.TEXT); if (mine) { team.setFont(FontManager.getRunescapeBoldFont()); }
        JLabel value = new JLabel(Ui.plain(score)); value.setForeground(mine ? Color.WHITE : Ui.MUTED); value.setFont(FontManager.getRunescapeSmallFont());
        row.add(place, BorderLayout.WEST); row.add(team, BorderLayout.CENTER); row.add(value, BorderLayout.EAST); return Ui.fit(row);
    }

    /** Enemy ship types (sunk or afloat), buoys found of each type, your shots and your own fleet. */
    private void fleet(JsonObject fleet, String key, Set<String> closed)
    {
        JsonArray ships = Json.array(fleet, "ships"); int sunk = 0;
        for (JsonElement value : ships) { if (value.isJsonObject() && Json.flag(value.getAsJsonObject(), "sunk")) { sunk++; } }
        String enemy = Json.text(fleet, "enemyName");
        JPanel body = Ui.section(this, key + "fleet", "Enemy fleet" + (enemy.isEmpty() ? "" : ": " + enemy), sunk + " of " + ships.size() + " sunk", closed);
        for (JsonElement value : ships)
        {
            if (!value.isJsonObject()) { continue; } JsonObject ship = value.getAsJsonObject(); boolean down = Json.flag(ship, "sunk");
            body.add(Ui.line(Ui.pill(down ? "Sunk" : "Afloat", down ? Ui.GOOD : Ui.MUTED), Ui.text(Json.text(ship, "label") + " (" + Json.number(ship, "length", 0) + ")", Ui.CARD_WIDTH - 60)));
        }
        JsonArray buoys = Json.array(fleet, "buoys");
        if (buoys.size() > 0)
        {
            Ui.gap(body, 4); JLabel title = Ui.muted("Buoys found", Ui.WIDTH); title.setForeground(Ui.GOLD); body.add(title);
            for (JsonElement value : buoys)
            {
                if (!value.isJsonObject()) { continue; } JsonObject buoy = value.getAsJsonObject();
                body.add(Ui.row(Json.text(buoy, "label"), Json.number(buoy, "found", 0) + " of " + Json.number(buoy, "total", 0), false));
            }
        }
        JsonObject shots = Json.object(fleet, "shots"), mine = Json.object(fleet, "yourFleet");
        Ui.gap(body, 4);
        body.add(Ui.row("Your shots", Json.number(shots, "hits", 0) + " hit, " + Json.number(shots, "misses", 0) + " miss", false));
        if (Json.number(mine, "total", 0) > 0) { body.add(Ui.row("Your fleet", Json.number(mine, "afloat", 0) + " of " + Json.number(mine, "total", 0) + " afloat", false)); }
    }
}
