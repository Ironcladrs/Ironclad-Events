package com.ironcladrs.events.api.dto;

import com.google.gson.*;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;

/** Defensive accessors keep missing optional server fields from breaking the sidebar. */
public final class Json
{
    private Json() {}
    public static JsonObject object(JsonObject parent, String key)
    {
        JsonElement value = parent.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }
    public static JsonArray array(JsonObject parent, String key)
    {
        JsonElement value = parent.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : new JsonArray();
    }
    public static String text(JsonObject parent, String key)
    {
        JsonElement value = parent.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }
    public static long number(JsonObject parent, String key, long fallback)
    {
        try { return parent.has(key) ? parent.get(key).getAsLong() : fallback; }
        catch (RuntimeException ex) { return fallback; }
    }
    public static int bounded(JsonObject parent, String key, int fallback, int min, int max)
    {
        return (int) Math.max(min, Math.min(max, number(parent, key, fallback)));
    }
    public static boolean flag(JsonObject parent, String key)
    {
        return parent.has(key) && parent.get(key).isJsonPrimitive() && "true".equals(parent.get(key).getAsString());
    }
    public static String norm(String value)
    {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
            .replace(' ', ' ').toLowerCase(Locale.ROOT).trim().replaceAll("[\\s_-]+", " ");
    }
    public static Instant time(String value)
    {
        try { return Instant.parse(value); } catch (RuntimeException ex) { return null; }
    }
    public static String safe(String value)
    {
        String text = value == null ? "" : value.replace("<", "").replace(">", "").replaceAll("[\\r\\n]", " ");
        return text.substring(0, Math.min(80, text.length()));
    }
    public static JsonObject obj(Object... pairs)
    {
        JsonObject object = new JsonObject();
        for (int i = 0; i < pairs.length; i += 2)
        {
            Object value = pairs[i + 1]; String key = (String) pairs[i];
            if (value instanceof JsonElement) { object.add(key, (JsonElement) value); }
            else if (value instanceof Number) { object.addProperty(key, (Number) value); }
            else if (value instanceof Boolean) { object.addProperty(key, (Boolean) value); }
            else if (value != null) { object.addProperty(key, value.toString()); }
        }
        return object;
    }
}
