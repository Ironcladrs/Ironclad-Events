package com.ironcladrs.events.pb;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.gameval.InventoryID;

/** The only worn-gear serializer. No inventory, bank or other player equipment is retained. */
@Singleton
public class GearSnapshot
{
    @Inject private Client client;
    public JsonObject capture()
    {
        JsonObject gear = new JsonObject(), levels = new JsonObject(); ItemContainer worn = client.getItemContainer(InventoryID.WORN);
        if (worn != null)
        {
            for (EquipmentInventorySlot slot : EquipmentInventorySlot.values())
            {
                if (slot == EquipmentInventorySlot.RING || slot == EquipmentInventorySlot.AMMO || slot == EquipmentInventorySlot.HEAD
                    || slot == EquipmentInventorySlot.CAPE || slot == EquipmentInventorySlot.AMULET || slot == EquipmentInventorySlot.WEAPON
                    || slot == EquipmentInventorySlot.BODY || slot == EquipmentInventorySlot.SHIELD || slot == EquipmentInventorySlot.LEGS
                    || slot == EquipmentInventorySlot.GLOVES || slot == EquipmentInventorySlot.BOOTS)
                { Item item = worn.getItem(slot.getSlotIdx()); if (item != null && item.getId() > 0) { gear.addProperty(slot.name(), item.getId()); } }
            }
        }
        for (Skill skill : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.RANGED, Skill.PRAYER, Skill.MAGIC})
        { levels.addProperty(skill.name(), client.getRealSkillLevel(skill)); }
        return Json.obj("gear", gear, "levels", levels);
    }
}
