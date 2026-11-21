package com.ironcladrs.events.progress;

import com.ironcladrs.events.api.dto.Json;
import java.util.*;
import java.util.regex.*;
import javax.inject.Singleton;

/** The two game-message formats match Dink; counts remain local until an event requests them. */
@Singleton
public class KillCountCollector
{
    private static final Pattern PRIMARY = Pattern.compile("Your (.+)\\s(kill|chest|completion|harvest|success|opened|lap|Total Ticket)\\s?count is: ?([\\d,]+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECONDARY = Pattern.compile("Your (?:completed|subdued) (.+) count is: ([\\d,]+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLUE = Pattern.compile("You have completed ([0-9,]+) (beginner|easy|medium|hard|elite|master) Treasure Trails?", Pattern.CASE_INSENSITIVE);
    private final Map<String, Count> counts = new HashMap<>();
    private Count latest;
    public Count accept(String message, int tick)
    {
        Count count = parse(message, tick);
        if (count != null) { counts.put(key(count.name), count); latest = count; } return count;
    }
    public static Count parse(String message, int tick)
    {
        Matcher primary = PRIMARY.matcher(message), secondary = SECONDARY.matcher(message);
        String name, value;
        if (primary.find()) { name = primary.group(1); value = primary.group(3); }
        else if (secondary.find()) { name = secondary.group(1); value = secondary.group(2); }
        else { Matcher clue = CLUE.matcher(message); if (!clue.find()) { return null; } name = "Clue Scroll (" + clue.group(2) + ")"; value = clue.group(1); }
        // These are the names Dink uses for Gauntlet's NPC-vs-event notifications.
        if (name.equalsIgnoreCase("Gauntlet")) { name = "Crystalline Hunllef"; }
        if (name.equalsIgnoreCase("Corrupted Gauntlet")) { name = "Corrupted Hunllef"; }
        try { return new Count(name, Integer.parseInt(value.replace(",", "")), tick); }
        catch (NumberFormatException ex) { return null; }
    }
    public Count recent(String source, int tick)
    {
        Count count = counts.get(key(source));
        if (count == null && latest != null && Json.norm(latest.name).startsWith(Json.norm(source) + ":") || count == null && latest != null && Json.norm(latest.name).startsWith(Json.norm(source) + " ")) { count = latest; }
        return count != null && tick >= count.tick && tick - count.tick <= 1 ? count : null;
    }
    public Count latest(int tick) { return latest != null && Math.abs(tick - latest.tick) <= 1 ? latest : null; }
    public Count get(String name) { return counts.get(key(name)); }
    public static String key(String name)
    {
        String normalized = Json.norm(name).replace(":", "").replace(" mode", "");
        if (normalized.equals("the gauntlet") || normalized.equals("gauntlet")) { return "crystalline hunllef"; }
        if (normalized.equals("the corrupted gauntlet") || normalized.equals("corrupted gauntlet")) { return "corrupted hunllef"; }
        return normalized.replaceAll("clue scrolls? ?\\(?([a-z]+)\\)?", "clue scroll $1");
    }
    public void reset() { counts.clear(); latest = null; }
    public static final class Count
    {
        public final String name; public final int count, tick;
        public Count(String name, int count, int tick) { this.name = name; this.count = count; this.tick = tick; }
    }
}
