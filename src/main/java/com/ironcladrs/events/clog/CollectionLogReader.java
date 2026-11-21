/*
BSD 2-Clause License

Copyright (c) 2021, andmcadams
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/
/*
BSD 2-Clause License

Copyright (c) 2022, Reinhardt Rijna
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ironcladrs.events.clog;

import com.google.gson.*;
import com.ironcladrs.events.account.AccountSnapshot;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.queue.Consent;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.*;
import net.runelite.client.eventbus.Subscribe;

/** Adapted from RuneProfile's CollectionLogWidgetSubscriber (BSD-2; NOTICE).
 * Script IDs are named here because gameval has no ScriptID constants for them. */
@Singleton
public class CollectionLogReader
{
    private static final int DELAYED_TRANSMIT = 4100, SETUP = 7797, INIT = 2240;
    @Inject private Client client;
    @Inject private Consent consent;
    @Inject private AccountSnapshot account;
    private JsonObject items = new JsonObject();
    private boolean fetching;
    private int lastTick = -1, startedTick = -1;
    public static boolean ownLog(int adventureLogFlag) { return adventureLogFlag == 0; }
    private boolean permitted()
    { return consent.allows("account") && ownLog(client.getVarbitValue(VarbitID.COLLECTION_POH_HOST_BOOK_OPEN)); }
    @Subscribe public void onScriptPostFired(ScriptPostFired event)
    {
        if (event.getScriptId() != SETUP || !permitted() || fetching) { return; }
        fetching = true; items = new JsonObject(); lastTick = -1; startedTick = client.getTickCount();
        client.menuAction(-1, InterfaceID.Collection.SEARCH_TOGGLE, MenuAction.CC_OP, 1, -1, "Search", null);
        client.runScript(INIT);
    }
    @Subscribe public void onScriptPreFired(ScriptPreFired event)
    {
        if (event.getScriptId() != DELAYED_TRANSMIT || !fetching || !permitted()) { return; }
        Object[] args = event.getScriptEvent().getArguments();
        if (args == null || args.length < 3 || !(args[1] instanceof Integer) || !(args[2] instanceof Integer)) { return; }
        int id = (Integer) args[1], quantity = (Integer) args[2];
        if (id > 0 && quantity >= 0 && items.size() < 5000) { items.addProperty(Integer.toString(id), quantity); lastTick = client.getTickCount(); }
    }
    @Subscribe public void onGameTick(GameTick event)
    {
        if (!permitted()) { reset(); return; }
        if (fetching && lastTick < 0 && client.getTickCount() > startedTick + 50) { reset(); return; }
        if (fetching && lastTick >= 0 && client.getTickCount() > lastTick + 2)
        {
            fetching = false; lastTick = -1;
            if (items.size() > 0)
            { account.part("collectionLog", Json.obj("items", items.deepCopy(), "uniqueObtained", client.getVarpValue(VarPlayerID.COLLECTION_COUNT),
                "uniqueTotal", client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX), "full", true)); }
        }
    }
    @Subscribe public void onVarbitChanged(VarbitChanged event)
    { if (event.getVarbitId() == VarbitID.COLLECTION_POH_HOST_BOOK_OPEN && !ownLog(client.getVarbitValue(VarbitID.COLLECTION_POH_HOST_BOOK_OPEN))) { reset(); } }
    public void reset() { fetching = false; items = new JsonObject(); lastTick = -1; }
}
