package com.ironcladrs.events;

import com.ironcladrs.events.ca.CombatAchievementReader;
import org.junit.Test;
import static org.junit.Assert.*;

public class CombatAchievementReaderTest
{
    @Test public void taskBitsAcrossWordAndSignBoundaries()
    {
        int[] words = {Integer.MIN_VALUE, 1};
        assertTrue(CombatAchievementReader.completed(words, 31)); assertTrue(CombatAchievementReader.completed(words, 32));
        assertFalse(CombatAchievementReader.completed(words, 30)); assertFalse(CombatAchievementReader.completed(words, 64)); assertFalse(CombatAchievementReader.completed(words, -1));
    }
}
