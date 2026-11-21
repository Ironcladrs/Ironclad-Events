package com.ironcladrs.events.ui;

import com.ironcladrs.events.IroncladEventsConfig;
import com.ironcladrs.events.sync.SessionState;
import java.awt.Component;
import javax.swing.*;

/** Consent is edited only through RuneLite configuration, never silently by the setup view. */
public final class SetupCard extends JPanel
{
    public SetupCard(IroncladEventsConfig config, SessionState state, IroncladPanel.Actions actions)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT);
        add(Ui.text(state.message));
        add(new ClanSettingsCard(config, state, actions));
        row("Connect", config.connect()); row("Key entered", !config.apiKey().trim().isEmpty()); row("This account linked", state.linked);
        row("Event drops: items, source, KC, time", config.autoDrops()); available(state, "loot");
        row("Screenshots: whole game window, including chat", config.screenshots());
        row("Bank checks: asked counts only, shown first", config.bankPrechecks()); available(state, "bankPrechecks");
        row("Live XP, KC and active event minutes", config.liveProgress()); available(state, "progress");
        row("Account: log, CAs, quests, diaries, levels, type", config.syncAccount()); available(state, "account");
        row("New PBs: time, worn gear, combat levels", config.autoPbs()); available(state, "pbs");
        row("Clan chat: sender, text, time", config.relayClanChat()); available(state, "chat");
        row("Own deaths: announcement only", config.autoDeaths()); available(state, "deaths");
        row("Death screenshots: published to clan Discord", config.deathScreenshots());
        row("New pets", config.sharePets()); available(state, "pets");
        row("Level milestones", config.shareMilestones()); available(state, "milestones");
        row("Clue counts", config.shareClues()); available(state, "clues");
        add(Ui.text("Enable collection-log notifications in the game's settings for new-item updates."));
        Ui.link(this, "Get my key", "https://ironcladrs.com/settings#plugin");
        add(Ui.button("Settings help", () -> JOptionPane.showMessageDialog(this,
            "Open RuneLite's Configuration panel, search for Ironclad Events, then click its settings cog.", "Ironclad Events settings", JOptionPane.INFORMATION_MESSAGE)));
        Ui.link(this, "What is sent", "https://ironcladrs.com/privacy#plugin");
    }
    private void row(String label, boolean value) { add(Ui.text((value ? "On: " : "Off: ") + label)); }
    private void available(SessionState state, String feature)
    { if (state.linked && !state.feature(feature)) { add(Ui.text("Not available yet")); } }
}
