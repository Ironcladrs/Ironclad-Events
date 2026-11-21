package com.ironcladrs.events;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * RuneLite cuts setting names at about 25 characters, so names stay short and each description says
 * exactly what is sent, when, and where it goes. Key names never change (that would reset saved settings).
 * Every switch that sends data defaults off and carries the third-party warning.
 */
@ConfigGroup(IroncladEventsConfig.GROUP)
public interface IroncladEventsConfig extends Config
{
    String GROUP = "ironcladevents";
    String THIRD_PARTY_WARNING = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers";

    String KEY_HELP = "Get it on ironcladrs.com: Settings, then My plugin data, then Create key. Or press Get my key in the Ironclad side panel.";

    // ── Sections ─────────────────────────────────────────────────────────────
    @ConfigSection(name = "1. Connect", description = "Link this plugin to your ironcladrs.com account. " + KEY_HELP, position = 0)
    String connectionSection = "connection";

    @ConfigSection(name = "2. Event drops", description = "File drops for live event tiles automatically, and send big drops to the Wall of Fame.", position = 1, closedByDefault = true)
    String dropsSection = "drops";

    @ConfigSection(name = "3. Clan loot filters", description = "Use the drop filters clan staff set on the website instead of your own.", position = 2, closedByDefault = true)
    String clanSection = "clan";

    @ConfigSection(name = "4. Prechecks and progress", description = "Answer bank prechecks from the panel and share live XP and kill counts during events.", position = 3, closedByDefault = true)
    String eventsSection = "events";

    @ConfigSection(name = "5. Account and PBs", description = "Keep your collection log, achievements and levels up to date on the site, and submit personal bests.", position = 4, closedByDefault = true)
    String accountSection = "account";

    @ConfigSection(name = "6. Clan chat", description = "Copy the Ironclad clan channel to the clan's Discord.", position = 5, closedByDefault = true)
    String chatSection = "chat";

    @ConfigSection(name = "7. Deaths", description = "Post your own deaths to the clan's Wall of Shame in Discord.", position = 6, closedByDefault = true)
    String deathsSection = "deaths";

    @ConfigSection(name = "8. Pets, levels, clues", description = "Share your pets, 99s and clue completions for clan announcements and stats.", position = 7, closedByDefault = true)
    String activitySection = "activity";

    @ConfigSection(name = "9. Notices and panel", description = "What the plugin shows you. Nothing here sends data.", position = 8, closedByDefault = true)
    String noticesSection = "notices";

    // ── 1. Connect ───────────────────────────────────────────────────────────
    @ConfigItem(section = connectionSection, keyName = "apiKey", name = "Plugin key", position = 0, secret = true,
        description = "Your personal key that links this plugin to your clan account. " + KEY_HELP + " Treat it like a password: never post it in chat or Discord.")
    default String apiKey() { return ""; }

    @ConfigItem(section = connectionSection, keyName = "connect", name = "Connect to ironcladrs.com", position = 1, warning = THIRD_PARTY_WARNING,
        description = "Main switch. Off: nothing is sent. On: checks your key and loads your events, team, tasks and claims into the side panel. Each upload in sections 2 to 8 also needs its own switch.")
    default boolean connect() { return false; }

    // ── 2. Event drops ───────────────────────────────────────────────────────
    @ConfigItem(section = dropsSection, keyName = "autoDrops", name = "Submit event drops", position = 0, warning = THIRD_PARTY_WARNING,
        description = "When you get a drop that matches a tile in a live event you are rostered in, file the claim for you: items, source, kill count and time. Also sends drops above the Wall of Fame minimum. Staff still review every claim.")
    default boolean autoDrops() { return false; }

    @ConfigItem(section = dropsSection, keyName = "screenshots", name = "Attach screenshots", position = 1, warning = THIRD_PARTY_WARNING,
        description = "Add a screenshot of your whole game window, chat included, to drops and personal bests as proof for staff.")
    default boolean screenshots() { return false; }

    @Range(min = 0)
    @ConfigItem(section = dropsSection, keyName = "minimumWallValue", name = "Wall of Fame min (gp)", position = 2,
        description = "Only send non-event drops worth at least this much for the Wall of Fame. 0 uses the clan's minimum. Event tile drops always send. Ignored while Use clan loot filters is on.")
    default int minimumWallValue() { return 0; }

    @ConfigItem(section = dropsSection, keyName = "ignoredLootItems", name = "Never send these items", position = 3,
        description = "Item names separated by commas, for example: Bones, Ashes. A drop made only of these items is not sent; a drop with anything else still sends in full. Careful: this can stop an event drop from filing. Ignored while Use clan loot filters is on.")
    default String ignoredLootItems() { return ""; }

    @ConfigItem(section = dropsSection, keyName = "ignoredLootSources", name = "Never send from these", position = 4,
        description = "Monster or activity names separated by commas, for example: Barrows, Tempoross. Nothing from them is sent. Careful: this can stop an event drop from filing. Ignored while Use clan loot filters is on.")
    default String ignoredLootSources() { return ""; }

    // ── 3. Clan loot filters ─────────────────────────────────────────────────
    @ConfigItem(section = clanSection, keyName = "useClanSettings", name = "Use clan loot filters", position = 0,
        description = "Replace your Wall of Fame minimum and both Never send lists with the ones clan staff set on the website. Your own values are kept for when you turn this off. Your upload switches never change.")
    default boolean useClanSettings() { return false; }

    // ── 4. Prechecks and progress ────────────────────────────────────────────
    @ConfigItem(section = eventsSection, keyName = "bankPrechecks", name = "Bank prechecks", position = 0, warning = THIRD_PARTY_WARNING,
        description = "When an event precheck asks how many of an item you own, open your bank and the side panel shows the exact counts it will send. Nothing is sent until you press Send. Only the asked items are counted, from bank, inventory and equipment.")
    default boolean bankPrechecks() { return false; }

    @ConfigItem(section = eventsSection, keyName = "liveProgress", name = "Live event progress", position = 1, warning = THIRD_PARTY_WARNING,
        description = "During events you are in, send XP and kill counts for the skills and bosses that event tracks, plus your logged-in minutes. The site shows it as waiting until Wise Old Man confirms it; scores only ever use Wise Old Man.")
    default boolean liveProgress() { return false; }

    // ── 5. Account and PBs ───────────────────────────────────────────────────
    @ConfigItem(section = accountSection, keyName = "syncAccount", name = "Sync my account", position = 0, warning = THIRD_PARTY_WARNING,
        description = "Send your collection log, combat achievements, quests, diaries, levels and account type when they change. Used for collection log events, precheck evidence and your site profile. Open your collection log once so it can be read.")
    default boolean syncAccount() { return false; }

    @ConfigItem(section = accountSection, keyName = "autoPbs", name = "Submit personal bests", position = 1, warning = THIRD_PARTY_WARNING,
        description = "When the game says you set a new personal best, send the time, team size, worn gear and combat levels to the clan PB ladder. Staff approve it before it shows.")
    default boolean autoPbs() { return false; }

    // ── 6. Clan chat ─────────────────────────────────────────────────────────
    @ConfigItem(section = chatSection, keyName = "relayClanChat", name = "Relay clan chat", position = 0, warning = THIRD_PARTY_WARNING,
        description = "Copy messages typed in the Ironclad clan channel (sender, text and time) to the clan's Discord. Guest, friends, public and private chat are never sent.")
    default boolean relayClanChat() { return false; }

    // ── 7. Deaths ────────────────────────────────────────────────────────────
    @ConfigItem(section = deathsSection, keyName = "autoDeaths", name = "Announce my deaths", position = 0, warning = THIRD_PARTY_WARNING,
        description = "Post your own deaths to the clan's Wall of Shame in Discord. No location, killer names or item losses are sent.")
    default boolean autoDeaths() { return false; }

    @ConfigItem(section = deathsSection, keyName = "deathScreenshots", name = "Death screenshots", position = 1, warning = THIRD_PARTY_WARNING,
        description = "Also post a screenshot of your game window, chat included, with each death. Anyone in the clan Discord channel can see it.")
    default boolean deathScreenshots() { return false; }

    // ── 8. Pets, levels, clues ───────────────────────────────────────────────
    @ConfigItem(section = activitySection, keyName = "sharePets", name = "Announce my pets", position = 0, warning = THIRD_PARTY_WARNING,
        description = "When you get a pet, send the game's pet message so the clan can celebrate it in Discord.")
    default boolean sharePets() { return false; }

    @ConfigItem(section = activitySection, keyName = "shareMilestones", name = "Announce 99s and totals", position = 1, warning = THIRD_PARTY_WARNING,
        description = "Send your new 99s and total-level milestones for clan announcements. Levels you already had are never announced.")
    default boolean shareMilestones() { return false; }

    @ConfigItem(section = activitySection, keyName = "shareClues", name = "Share clue completions", position = 2, warning = THIRD_PARTY_WARNING,
        description = "Send the tier and your completion count each time you finish a clue, for clan clue stats.")
    default boolean shareClues() { return false; }

    // ── 9. Notices and panel ─────────────────────────────────────────────────
    @ConfigItem(section = noticesSection, keyName = "chatNotices", name = "Chat messages", position = 0,
        description = "Show a game chat message when a claim is filed, approved or denied. Nothing is sent.")
    default boolean chatNotices() { return true; }

    @ConfigItem(section = noticesSection, keyName = "desktopNotices", name = "Desktop notifications", position = 1,
        description = "Show a desktop notification for claim decisions and event updates. Nothing is sent.")
    default boolean desktopNotices() { return true; }

    @ConfigItem(section = noticesSection, keyName = "showPanel", name = "Show side panel", position = 2,
        description = "Show the Ironclad Events button in RuneLite's sidebar. Nothing is sent.")
    default boolean showPanel() { return true; }
}
