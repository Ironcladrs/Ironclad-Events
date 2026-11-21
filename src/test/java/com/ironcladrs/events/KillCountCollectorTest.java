package com.ironcladrs.events;

import com.ironcladrs.events.progress.KillCountCollector;
import org.junit.Test;
import static org.junit.Assert.*;

public class KillCountCollectorTest
{
    @Test public void realBossAndRaidLines()
    {
        String[][] rows = {{"Your Sol Heredit kill count is: 14.", "Sol Heredit", "14"},
            {"Your completed Chambers of Xeric count is: 1,234.", "Chambers of Xeric", "1234"},
            {"Your subdued Wintertodt count is: 7.", "Wintertodt", "7"},
            {"Your completed Tombs of Amascut: Expert Mode count is: 12.", "Tombs of Amascut: Expert Mode", "12"}};
        for (String[] row : rows)
        { KillCountCollector.Count count = KillCountCollector.parse(row[0], 10); assertNotNull(row[0], count); assertEquals(row[1], count.name); assertEquals(Integer.parseInt(row[2]), count.count); }
        assertNull(KillCountCollector.parse("A friend killed Sol Heredit.", 10));
    }
    @Test public void killCountOnlyAttachesSameOrPreviousTick()
    {
        KillCountCollector collector = new KillCountCollector(); collector.accept("Your Sol Heredit kill count is: 14.", 10);
        assertNotNull(collector.recent("sol heredit", 10)); assertNotNull(collector.recent("Sol Heredit", 11)); assertNull(collector.recent("Sol Heredit", 12)); assertNull(collector.recent("Zulrah", 10));
    }
}
