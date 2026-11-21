package com.ironcladrs.events.ca;

import com.google.gson.*;
import com.ironcladrs.events.api.dto.Json;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.gameval.VarPlayerID;

/** Task enums and param 1306 are used by the game's ca_tasks_draw_list/4834 scripts.
 * There are no gameval enum/param constants for these in RuneLite 1.13.6. */
@Singleton
public class CombatAchievementReader
{
    private static final int TASK_ID_PARAM = 1306;
    private static final int[] TIER_ENUMS = {3981, 3982, 3983, 3984, 3985, 3986};
    private static final String[] TIERS = {"EASY", "MEDIUM", "HARD", "ELITE", "MASTER", "GRANDMASTER"};
    private static final int[] VARPS = {
        VarPlayerID.CA_TASK_COMPLETED_0, VarPlayerID.CA_TASK_COMPLETED_1, VarPlayerID.CA_TASK_COMPLETED_2,
        VarPlayerID.CA_TASK_COMPLETED_3, VarPlayerID.CA_TASK_COMPLETED_4, VarPlayerID.CA_TASK_COMPLETED_5,
        VarPlayerID.CA_TASK_COMPLETED_6, VarPlayerID.CA_TASK_COMPLETED_7, VarPlayerID.CA_TASK_COMPLETED_8,
        VarPlayerID.CA_TASK_COMPLETED_9, VarPlayerID.CA_TASK_COMPLETED_10, VarPlayerID.CA_TASK_COMPLETED_11,
        VarPlayerID.CA_TASK_COMPLETED_12, VarPlayerID.CA_TASK_COMPLETED_13, VarPlayerID.CA_TASK_COMPLETED_14,
        VarPlayerID.CA_TASK_COMPLETED_15, VarPlayerID.CA_TASK_COMPLETED_16, VarPlayerID.CA_TASK_COMPLETED_17,
        VarPlayerID.CA_TASK_COMPLETED_18, VarPlayerID.CA_TASK_COMPLETED_19, VarPlayerID.CA_TASK_COMPLETED_20};
    @Inject private Client client;
    public JsonObject capture()
    {
        int[] bits = new int[VARPS.length]; for (int i = 0; i < VARPS.length; i++) { bits[i] = client.getVarpValue(VARPS[i]); }
        JsonObject tiers = new JsonObject(); JsonArray completed = new JsonArray();
        for (int tier = 0; tier < TIERS.length; tier++)
        {
            EnumComposition definitions = client.getEnum(TIER_ENUMS[tier]);
            if (definitions == null || definitions.getIntVals() == null) { continue; }
            int done = 0, total = 0;
            for (int structId : definitions.getIntVals())
            {
                int taskId = client.getStructComposition(structId).getIntValue(TASK_ID_PARAM);
                if (taskId < 0 || taskId / 32 >= bits.length) { continue; }
                total++;
                if (completed(bits, taskId)) { done++; completed.add(taskId); }
            }
            JsonArray progress = new JsonArray(); progress.add(done); progress.add(total); tiers.add(TIERS[tier], progress);
        }
        return Json.obj("tiers", tiers, "completedTaskIds", completed);
    }
    public static boolean completed(int[] bits, int taskId)
    { return taskId >= 0 && taskId / 32 < bits.length && (bits[taskId / 32] & (1 << (taskId % 32))) != 0; }
}
