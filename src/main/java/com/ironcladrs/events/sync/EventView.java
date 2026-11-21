package com.ironcladrs.events.sync;

import com.google.gson.JsonObject;
import com.ironcladrs.events.api.dto.Json;

/** Additive fields are safe; unfamiliar view contracts retain only a site handoff. */
public final class EventView
{
    public static final int VERSION = 1;
    private EventView() {}
    public static boolean supported(JsonObject event)
    {
        if (!event.has("view")) { return true; } // Existing v1 servers predate the envelope.
        JsonObject view = Json.object(event, "view");
        return currentVersion(view) && "standard".equals(Json.text(view, "mode"));
    }
    private static boolean currentVersion(JsonObject view)
    {
        return view.has("version") && view.get("version").isJsonPrimitive()
            && view.getAsJsonPrimitive("version").isNumber() && Integer.toString(VERSION).equals(Json.text(view, "version"));
    }
    public static String message(JsonObject event)
    {
        JsonObject view = Json.object(event, "view");
        if (currentVersion(view) && "generic".equals(Json.text(view, "mode")))
        {
            String message = Json.text(view, "message");
            if (!message.isEmpty()) { return message; }
        }
        return "Open this event on the website for its instructions and progress. Automatic event reporting is paused for this view.";
    }
}
