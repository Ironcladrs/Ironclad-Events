package com.ironcladrs.events;

import com.ironcladrs.events.pb.PersonalBestListener;
import org.junit.Test;
import static org.junit.Assert.*;

public class PersonalBestListenerTest
{
    @Test public void parsesOnlyNewRecords()
    {
        assertEquals(Long.valueOf(83400), PersonalBestListener.parseNewBest("Duration: 1:23.40 (new personal best)"));
        assertEquals(Long.valueOf(732300), PersonalBestListener.parseNewBest("Challenge time: 12:12.30 (new personal best)"));
        assertEquals(Long.valueOf(14400), PersonalBestListener.parseNewBest("Subdued in: 0:14.40 (new personal best)"));
        assertNull(PersonalBestListener.parseNewBest("Duration: 1:30. Personal best: 1:23.40"));
        assertNull(PersonalBestListener.parseNewBest("Personal best: 1:23.40"));
        assertNull(PersonalBestListener.parseNewBest("Duration: garbage (new personal best)"));
    }
}
