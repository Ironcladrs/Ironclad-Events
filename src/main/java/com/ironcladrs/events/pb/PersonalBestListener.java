package com.ironcladrs.events.pb;

import com.google.gson.*;
import com.ironcladrs.events.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.capture.Screenshots;
import com.ironcladrs.events.notice.ChatNotices;
import com.ironcladrs.events.progress.KillCountCollector;
import com.ironcladrs.events.queue.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.regex.*;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/** Pair completion timing with the game's KC line; never infer a PB during combat. */
@Singleton
public class PersonalBestListener
{
    private static final Pattern TIME = Pattern.compile("(?:Duration|time|Subdued in):? ([0-9:]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
    @Inject private Client client;
    @Inject private IroncladEventsConfig config;
    @Inject private PlayerGuard guard;
    @Inject private Consent consent;
    @Inject private Outbox outbox;
    @Inject private KillCountCollector kc;
    @Inject private GearSnapshot gear;
    @Inject private Screenshots screenshots;
    @Inject private ChatNotices notices;
    private JsonObject pending;
    private int pendingTick;
    private final Set<String> sent = new HashSet<>();
    @Subscribe public void onChatMessage(ChatMessage event)
    {
        if (!consent.allows("pbs")) { return; }
        if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.FRIENDSCHATNOTIFICATION) { return; }
        String text = Text.removeTags(event.getMessage()); Long time = parseNewBest(text);
        if (time == null || text.startsWith("Preparation")) { return; }
        // TOB's wave time can precede challenge time in the same message.
        int challenge = text.indexOf("Theatre of Blood");
        if (text.startsWith("Wave") && challenge >= 0) { time = parseNewBest(text.substring(challenge)); if (time == null) { return; } }
        if (pending != null && pendingTick == client.getTickCount()) { return; }
        pending = gear.capture(); pending.addProperty("timeMs", time); pending.addProperty("at", Instant.now().toString()); pendingTick = client.getTickCount();
    }
    @Subscribe public void onGameTick(GameTick event)
    {
        if (pending == null) { return; }
        int tick = client.getTickCount();
        if (!consent.allows("pbs") || tick - pendingTick > 1) { pending = null; return; }
        KillCountCollector.Count count = kc.latest(tick);
        if (count == null) { return; }
        JsonObject payload = pending; pending = null;
        payload.addProperty("boss", count.name); payload.addProperty("killCount", count.count); payload.addProperty("teamSize", teamSize(count.name));
        String unique = count.name + ":" + count.count + ":" + Json.number(payload, "timeMs", 0);
        if (!sent.add(unique)) { return; }
        if (sent.size() > 100) { sent.clear(); sent.add(unique); }
        long generation = guard.generation();
        Consumer<byte[]> send = image -> {
            if (generation == guard.generation() && consent.allows("pbs"))
            { outbox.submit("pbs", "/pb", payload, image, result -> { if (result.success() && result.status != 409 && !Json.flag(result.data, "duplicate")) { notices.pb(count.name, format(Json.number(payload, "timeMs", 0))); } }); }
        };
        if (config.screenshots()) { screenshots.capture(send); } else { send.accept(null); }
    }
    public static Long parseNewBest(String text)
    {
        // "Personal best: 1:23" alone reports an old PB; only the new-PB marker proves a new record.
        if (!text.toLowerCase(Locale.ROOT).contains("(new personal best)")) { return null; }
        Matcher matcher = TIME.matcher(text); if (!matcher.find()) { return null; }
        try
        {
            String[] components = matcher.group(1).split(":"); if (components.length > 3) { return null; }
            BigDecimal seconds = BigDecimal.ZERO;
            for (String part : components) { seconds = seconds.multiply(BigDecimal.valueOf(60)).add(new BigDecimal(part)); }
            long ms = seconds.multiply(BigDecimal.valueOf(1000)).longValueExact(); return ms > 0 && ms < 86_400_000 ? ms : null;
        }
        catch (ArithmeticException | NumberFormatException ex) { return null; }
    }
    private int teamSize(String name)
    {
        if (name.startsWith("Chambers of Xeric")) { return Math.max(1, Math.min(100, client.getVarbitValue(VarbitID.RAIDS_CLIENT_PARTYSIZE))); }
        int[] members = name.startsWith("Tombs of Amascut")
            ? new int[]{VarbitID.TOA_CLIENT_P1, VarbitID.TOA_CLIENT_P2, VarbitID.TOA_CLIENT_P3, VarbitID.TOA_CLIENT_P4, VarbitID.TOA_CLIENT_P5, VarbitID.TOA_CLIENT_P6, VarbitID.TOA_CLIENT_P7}
            : name.startsWith("Theatre of Blood") ? new int[]{VarbitID.TOB_CLIENT_P1, VarbitID.TOB_CLIENT_P2, VarbitID.TOB_CLIENT_P3, VarbitID.TOB_CLIENT_P4} : new int[0];
        int size = 1; for (int varbit : members) { if (client.getVarbitValue(varbit) != 0) { size++; } } return size;
    }
    public static String format(long ms) { return (ms / 60000) + ":" + String.format(Locale.ROOT, "%06.3f", (ms % 60000) / 1000.0); }
    public void reset() { pending = null; sent.clear(); }
}
