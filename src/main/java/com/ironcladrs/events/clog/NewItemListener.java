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
package com.ironcladrs.events.clog;

import com.google.gson.JsonObject;
import com.ironcladrs.events.account.AccountSnapshot;
import com.ironcladrs.events.api.dto.Json;
import com.ironcladrs.events.queue.Consent;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;

/** Index collection-log item names in bounded chunks, including untradeables.
 * A one-item update is partial, so it never erases the last full log. */
@Singleton
public class NewItemListener
{
    private static final String PREFIX = "New item added to your collection log: ";
    @Inject private Consent consent;
    @Inject private Client client;
    @Inject private ItemManager items;
    @Inject private AccountSnapshot account;
    private final Map<String, Integer> ids = new HashMap<>();
    private final Set<String> waiting = new LinkedHashSet<>();
    // Cache enum/struct params used by the game's collection-log tabs. These
    // have no gameval aliases in RuneLite 1.13.6 (WikiSync; see NOTICE).
    private static final int TABS = 2102, SUBTABS_PARAM = 683, ITEMS_PARAM = 690, REPLACEMENTS = 3721;
    private Queue<Integer> toIndex;
    private Set<Integer> collectionIds()
    {
        EnumComposition tabs = client.getEnum(TABS);
        if (tabs == null || tabs.getIntVals() == null) { return null; }
        Set<Integer> result = new LinkedHashSet<>();
        for (int tabId : tabs.getIntVals())
        {
            StructComposition tab = client.getStructComposition(tabId); if (tab == null) { return null; }
            EnumComposition pages = client.getEnum(tab.getIntValue(SUBTABS_PARAM));
            if (pages == null || pages.getIntVals() == null) { return null; }
            for (int pageId : pages.getIntVals())
            {
                StructComposition page = client.getStructComposition(pageId); if (page == null) { return null; }
                EnumComposition definitions = client.getEnum(page.getIntValue(ITEMS_PARAM));
                if (definitions == null || definitions.getIntVals() == null) { return null; }
                for (int id : definitions.getIntVals()) { if (id > 0) { result.add(id); } }
            }
        }
        EnumComposition replacements = client.getEnum(REPLACEMENTS);
        if (replacements != null && replacements.getKeys() != null && replacements.getIntVals() != null)
        {
            for (int id : replacements.getKeys()) { result.remove(id); }
            for (int id : replacements.getIntVals()) { if (id > 0) { result.add(id); } }
        }
        return result;
    }
    @Subscribe public void onGameTick(GameTick event)
    {
        if (!consent.allows("account")) { return; }
        if (toIndex == null)
        {
            Set<Integer> eligible = collectionIds(); if (eligible == null) { return; }
            toIndex = new ArrayDeque<>(eligible);
        }
        for (int n = 0; n < 512 && !toIndex.isEmpty(); n++)
        {
            ItemComposition item = items.getItemComposition(toIndex.remove());
            if (item.getName() != null && item.getNote() == -1 && item.getPlaceholderTemplateId() == -1)
            { ids.putIfAbsent(Json.norm(item.getMembersName()), item.getId()); }
        }
        for (String name : new ArrayList<>(waiting)) { if (send(name)) { waiting.remove(name); } }
    }
    @Subscribe public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM) { return; }
        if (!consent.allows("account")) { return; }
        String text = Text.removeTags(event.getMessage()); if (!text.startsWith(PREFIX)) { return; }
        String name = Json.norm(text.substring(PREFIX.length()));
        if (!send(name) && waiting.size() < 100) { waiting.add(name); }
    }
    private boolean send(String name)
    {
        Integer id = ids.get(name); if (id == null) { return false; }
        JsonObject found = new JsonObject(); found.addProperty(Integer.toString(items.canonicalize(id)), 1);
        account.part("collectionLog", Json.obj("items", found, "full", false, "newItem", true)); return true;
    }
    public void reset() { waiting.clear(); }
}
