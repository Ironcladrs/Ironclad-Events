package com.ironcladrs.events;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.sync.SessionState;
import org.junit.Test;
import static org.junit.Assert.*;

public class SessionStateTest
{
    @Test public void remoteListsAreCappedAndPublishedStateCannotBeMutated()
    {
        JsonArray tasks = new JsonArray(), events = new JsonArray();
        for (int i = 0; i < 400; i++) { tasks.add(Json.obj("label", "Task " + i)); }
        for (int i = 0; i < 40; i++) { events.add(Json.obj("id", i, "tasks", tasks)); }
        JsonObject payload = Json.obj("events", events);
        SessionState state = new SessionState(true, "Connected", null, Json.obj("features", Json.obj("loot", true)), payload);
        payload.remove("events"); assertEquals(30, state.events().size());
        assertEquals(300, Json.array(state.events().get(0).getAsJsonObject(), "tasks").size());
        state.sync().remove("events"); state.hello().remove("features");
        assertEquals(30, state.events().size()); assertTrue(state.feature("loot"));
    }
}
