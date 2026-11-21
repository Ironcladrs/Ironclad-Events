package com.ironcladrs.events;

import com.google.gson.*;
import com.ironcladrs.events.precheck.BankAttestor;
import java.util.*;
import net.runelite.api.Item;
import org.junit.Test;
import static org.junit.Assert.*;

public class BankAttestorTest
{
    @Test public void foldsNotedAndPlaceholderFormsButExportsOnlyAskedIds()
    {
        JsonArray asked = new JsonArray(); asked.add(4151); asked.add(6737);
        JsonObject result = BankAttestor.count(asked, Arrays.asList(
            new Item[]{new Item(4152, 3), new Item(9999, 0), new Item(995, 1000000)},
            new Item[]{new Item(4151, 2)}, new Item[]{new Item(6737, 1)}), id -> id == 4152 || id == 9999 ? 4151 : id);
        assertEquals(2, result.size()); assertEquals(5, result.get("4151").getAsInt()); assertEquals(1, result.get("6737").getAsInt()); assertFalse(result.has("995"));
    }
    @Test public void absentAskedItemIsExplicitZero()
    { JsonArray asked = new JsonArray(); asked.add(4151); assertEquals(0, BankAttestor.count(asked, Collections.singletonList(new Item[0]), id -> id).get("4151").getAsInt()); }
}
