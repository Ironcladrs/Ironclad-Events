package com.ironcladrs.events.queue;

import com.ironcladrs.events.*;
import com.ironcladrs.events.sync.SyncService;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Rechecked for every attempt, including jobs captured before a switch changed. */
@Singleton
public class Consent
{
    @Inject private PlayerGuard guard;
    @Inject private IroncladEventsConfig config;
    @Inject private SyncService sync;
    public boolean allows(String feature)
    {
        guard.refresh();
        if (!config.connect() || guard.identity() == null || !sync.current() || !sync.state().feature(feature)) { return false; }
        switch (feature)
        {
            case "loot": return config.autoDrops();
            case "prechecks": return true;
            case "bankPrechecks": return config.bankPrechecks();
            case "captain": return true;
            case "progress": return config.liveProgress();
            case "account": return config.syncAccount();
            case "pbs": return config.autoPbs();
            case "chat": return config.relayClanChat();
            case "deaths": return config.autoDeaths();
            case "pets": return config.sharePets();
            case "milestones": return config.shareMilestones();
            case "clues": return config.shareClues();
            default: return false;
        }
    }
}
