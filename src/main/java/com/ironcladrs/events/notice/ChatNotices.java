package com.ironcladrs.events.notice;

import com.google.gson.*;
import com.ironcladrs.events.IroncladEventsConfig;
import com.ironcladrs.events.api.dto.Json;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.Notifier;

/** Only fixed templates go into RuneLite chat; remote values cannot inject formatting. */
@Singleton
public class ChatNotices
{
    @Inject private Client client;
    @Inject private Notifier notifier;
    @Inject private IroncladEventsConfig config;
    private final LinkedHashSet<String> seen = new LinkedHashSet<>();
    private boolean initialized;
    private final LinkedHashSet<String> recordedClaims = new LinkedHashSet<>();
    public void reset() { seen.clear(); recordedClaims.clear(); initialized = false; }
    public void line(String text)
    { if (config.chatNotices()) { client.addChatMessage(ChatMessageType.CONSOLE, "", "Ironclad: " + text, null); } }
    public void queued(int count) { line("couldn't reach the site; " + count + " item(s) queued."); }
    public void filed(String item, String label) { line("filed " + Json.safe(item) + " for " + Json.safe(label) + ". Waiting for staff."); }
    public void filed(String item, String label, String claimKey)
    {
        if (!recordedClaims.add(claimKey)) { return; }
        while (recordedClaims.size() > 500) { recordedClaims.remove(recordedClaims.iterator().next()); }
        filed(item,label);
    }
    public void pb(String boss, String time) { line("new personal best at " + Json.safe(boss) + ", " + Json.safe(time) + ", sent for approval."); }
    public void accept(JsonArray notices)
    {
        int n = 0;
        for (JsonElement value : notices)
        {
            if (++n > 50 || !value.isJsonObject()) { break; }
            JsonObject notice = value.getAsJsonObject(); String id = Json.text(notice, "id");
            if (id.isEmpty() || !seen.add(id) || !initialized) { continue; }
            String kind = Json.text(notice, "kind"), item = Json.safe(Json.text(notice, "item")), label = Json.safe(Json.text(notice, "label"));
            String reason = Json.safe(Json.text(notice, "reason"));
            switch (kind)
            {
                case "claim_recorded": filed(item, label, Json.text(notice, "claimKind") + ":" + Json.text(notice, "claimId")); break;
                case "claim_approved":
                    JsonArray lines = Json.array(notice, "lines");
                    line(item + " approved for " + label + ". " + (lines.size() > 0 ? Json.safe(lines.get(0).getAsString()) : "")); break;
                case "claim_denied": line(item + " for " + label + " was denied: " + reason); break;
                case "precheck_passed": line("precheck \"" + label + "\" passed."); break;
                case "precheck_needs_review": line("precheck \"" + label + "\" needs a staff look."); break;
                case "progress_mismatch": line("WOM hasn't caught up; log out to the lobby to update the hiscores."); break;
                default: break;
            }
            if (config.desktopNotices() && Arrays.asList("event_starting", "event_started", "door_open", "priority_changed", "claim_approved", "claim_denied", "pb_approved", "pb_denied").contains(kind))
            { notifier.notify("Ironclad: " + kind.replace('_', ' ') + (label.isEmpty() ? "" : " · " + label)); }
        }
        initialized = true;
        while (seen.size() > 500) { seen.remove(seen.iterator().next()); }
    }
}
