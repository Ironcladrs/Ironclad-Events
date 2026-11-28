/*
 * Safe-death rules adapted from Dink (pajlads/DinkPlugin, dinkplugin.util.WorldUtils),
 * Copyright (c) 2022, Jake Barter. BSD 2-Clause License; reproduced in NOTICE.
 */
package com.ironcladrs.events.death;

import com.ironcladrs.events.SafeDeathException;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

/**
 * Is a death in this place safe (nothing lost, so not Wall of Shame material)? Dink's list: safe
 * minigames and activities, raids and the Gauntlet for non-hardcore accounts, and the daily diary
 * revive at the Fight Caves and Zulrah. Anything on a hardcore world, or a hardcore account in the
 * hardcore-only cases, is always dangerous. The member (or clan) can choose safe deaths that still count.
 */
public final class SafeDeaths
{
    private SafeDeaths() {}
    private static final Set<Integer> BA_REGIONS = Set.of(7508, 7509, 10322);
    private static final Set<Integer> CASTLE_WARS_REGIONS = Set.of(9520, 9620);
    private static final Set<Integer> CLAN_WARS_REGIONS = Set.of(12621, 12622, 12623, 13130, 13131, 13133, 13134, 13135, 13386, 13387, 13390, 13641, 13642, 13643, 13644, 13645, 13646, 13647, 13899, 13900, 14155, 14156);
    private static final Set<Integer> COX_REGIONS = Set.of(12889, 13136, 13137, 13138, 13139, 13140, 13141, 13145, 13393, 13394, 13395, 13396, 13397, 13401);
    private static final Set<Integer> GALVEK_REGIONS = Set.of(6486, 6487, 6488, 6489, 6742, 6743, 6744, 6745);
    private static final Set<Integer> GAUNTLET_REGIONS = Set.of(7512, 7768, 12127);
    private static final Set<Integer> LMS_REGIONS = Set.of(13658, 13659, 13660, 13914, 13915, 13916, 13918, 13919, 13920, 14174, 14175, 14176, 14430, 14431, 14432);
    private static final Set<Integer> POH_REGIONS = Set.of(7257, 7534, 7535, 7790, 7791, 8046, 8047, 8302, 8303);
    private static final Set<Integer> SOUL_REGIONS = Set.of(8493, 8748, 8749, 9005);
    private static final Set<Integer> TOA_REGIONS = Set.of(14160, 14162, 14164, 14674, 14676, 15184, 15186, 15188, 15696, 15698, 15700);
    private static final Set<Integer> TOB_REGIONS = Set.of(12611, 12612, 12613, 12867, 12869, 13122, 13123, 13125, 13379);
    private static final Set<Integer> ZULRAH_REGIONS = Set.of(9007, 9008);
    private static final int INFERNO_REGION = 9043, CREATURE_GRAVEYARD_REGION = 13462, NMZ_REGION = 9033, TZHAAR_CAVE = 9551, CLAN_HALL = 6997, TZHAAR_PIT = 9552;
    private static final int DRAGON_SLAYER_II_COMPLETED = 215;

    /** True when a death at `regionId` should not be announced. */
    public static boolean safe(Client client, int regionId, Set<SafeDeathException> stillCount)
    {
        boolean hardcoreWorld = client.getWorldType().contains(WorldType.HARDCORE_MAIN);
        int ironman = client.getVarbitValue(VarbitID.IRONMAN);
        boolean hardcore = ironman == 3 || ironman == 5, hardcoreGroup = ironman == 5;
        if (GAUNTLET_REGIONS.contains(regionId)) { return !(hardcoreWorld || hardcore) && !stillCount.contains(SafeDeathException.GAUNTLET); }
        if (TOA_REGIONS.contains(regionId) || TOB_REGIONS.contains(regionId)) { return !(hardcoreWorld || hardcore) && !stillCount.contains(SafeDeathException.RAIDS); }
        if (COX_REGIONS.contains(regionId)) { return !(hardcoreWorld || hardcoreGroup) && !stillCount.contains(SafeDeathException.RAIDS); }
        if (regionId == INFERNO_REGION)
        {
            boolean jad = client.getVarpValue(VarPlayerID.SA_ENERGY_STORED) > 0;
            return !(hardcoreWorld || hardcoreGroup) && !stillCount.contains(jad ? SafeDeathException.JAD_CHALLENGES : SafeDeathException.INFERNO);
        }
        if (regionId == TZHAAR_CAVE)
        {
            if (hardcoreWorld || hardcoreGroup || stillCount.contains(SafeDeathException.FIGHT_CAVE)) { return false; }
            boolean revive = client.getVarbitValue(VarbitID.KARAMJA_FIGHTCAVE_RESSURECTION) == 0 && client.getVarbitValue(VarbitID.KARAMJA_DIARY_ELITE_COMPLETE) > 0;
            return !(revive && stillCount.contains(SafeDeathException.DIARY_REVIVE));
        }
        if (ZULRAH_REGIONS.contains(regionId) && client.getVarbitValue(VarbitID.ZULRAH_REVIVE) == 0 && client.getVarbitValue(VarbitID.WESTERN_DIARY_ELITE_COMPLETE) > 0)
        { return !stillCount.contains(SafeDeathException.DIARY_REVIVE); }
        if (BA_REGIONS.contains(regionId) || regionId == NMZ_REGION || regionId == CREATURE_GRAVEYARD_REGION || visible(client, InterfaceID.PestStatusOverlay.PEST_STATUS_PORT2)
            || (GALVEK_REGIONS.contains(regionId) && client.getVarbitValue(VarbitID.DS2) >= DRAGON_SLAYER_II_COMPLETED) || POH_REGIONS.contains(regionId))
        { return !(hardcoreWorld || hardcoreGroup); }
        return CASTLE_WARS_REGIONS.contains(regionId) || regionId == CLAN_HALL || CLAN_WARS_REGIONS.contains(regionId) || SOUL_REGIONS.contains(regionId)
            || LMS_REGIONS.contains(regionId) || visible(client, InterfaceID.BrOverlay.CONTENT) || regionId == TZHAAR_PIT;
    }
    private static boolean visible(Client client, int component)
    { Widget widget = client.getWidget(component); return widget != null && !widget.isHidden(); }
}
