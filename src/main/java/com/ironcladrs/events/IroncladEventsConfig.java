package com.ironcladrs.events;

import java.util.EnumSet;
import java.util.Set;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * RuneLite cuts setting names at about 25 characters, so names stay short and each description says
 * exactly what is sent, when, and where it goes. Connect is the single gate for all traffic and carries
 * the third-party warning; nothing below it can send while it is off, and every upload starts off.
 */
@ConfigGroup(IroncladEventsConfig.GROUP)
public interface IroncladEventsConfig extends Config
{
    String GROUP = "ironcladevents";
    // RuneLite asks the warning to say what is sent (wiki: Information about the Plugin Hub).
    String THIRD_PARTY_WARNING = "This feature submits your IP address, your RuneScape name, your account's id, your world type and which of these switches are on to ironcladrs.com, a 3rd-party server not controlled or verified by RuneLite developers."
        + " Each switch below sends only what it describes once you turn it on (drops, screenshots, collection log, combat achievements, quests, levels and account type, personal bests, deaths, pets, clue completions and, if enabled, clan channel messages)."
        + " See ironcladrs.com/privacy.";

    String KEY_HELP = "Get it on ironcladrs.com: Settings, then My plugin data, then Create key. Or press Get my key in the Ironclad side panel.";

    // ── Sections ─────────────────────────────────────────────────────────────
    @ConfigSection(name = "1. Connect", description = "Link this plugin to your ironcladrs.com account. " + KEY_HELP, position = 0)
    String connectionSection = "connection";

    @ConfigSection(name = "2. Drops", description = "Which drops are sent: event tiles, the Wall of Fame, screenshots and filters.", position = 1, closedByDefault = true)
    String dropsSection = "drops";

    @ConfigSection(name = "3. Clan filters", description = "Use the filters clan staff set on the website instead of your own: drops, screenshots, levels, clues, deaths and clan chat.", position = 2, closedByDefault = true)
    String clanSection = "clan";

    @ConfigSection(name = "4. Prechecks and progress", description = "Answer bank prechecks from the panel and share live XP and kill counts during events.", position = 3, closedByDefault = true)
    String eventsSection = "events";

    @ConfigSection(name = "5. Account sync", description = "Keep parts of your account up to date on the site. Each part is separate.", position = 4, closedByDefault = true)
    String accountSection = "account";

    @ConfigSection(name = "6. Personal bests", description = "Submit new personal bests to the clan PB ladder.", position = 5, closedByDefault = true)
    String pbSection = "pbs";

    @ConfigSection(name = "7. Clan chat", description = "Copy the Ironclad clan channel to the clan's Discord.", position = 6, closedByDefault = true)
    String chatSection = "chat";

    @ConfigSection(name = "8. Deaths", description = "Post your own deaths to the clan's Wall of Shame in Discord.", position = 7, closedByDefault = true)
    String deathsSection = "deaths";

    @ConfigSection(name = "9. Pets, levels, clues", description = "Share your pets, 99s and clue completions for clan announcements and stats.", position = 8, closedByDefault = true)
    String activitySection = "activity";

    @ConfigSection(name = "10. Notices and panel", description = "What the plugin shows you. Nothing here sends data.", position = 9, closedByDefault = true)
    String noticesSection = "notices";

    /** Every switch that sends data. The panel's Enable clan defaults may only touch these keys. */
    String[] UPLOAD_KEYS = {"autoDrops", "wallOfFameDrops", "screenshots", "bankPrechecks", "liveProgress", "syncCollectionLog", "syncCombatAchievements",
        "syncQuests", "syncLevels", "autoPbs", "pbScreenshots", "relayClanChat", "autoDeaths", "deathScreenshots", "sharePets", "shareMilestones", "shareClues",
        "shareCombatTasks", "shareDiaries", "sendPkLoot"};

    /** Plain-word label for each upload switch, in UPLOAD_KEYS order, for the panel and the clan-defaults dialog. */
    String[] UPLOAD_LABELS = {"Event tile drops", "Wall of Fame drops", "Screenshots on drops", "Bank prechecks", "Live event progress", "Collection log",
        "Combat achievements", "Quests and diaries", "Levels and account type", "Personal bests", "Screenshots on PBs", "Clan chat relay",
        "Announce my deaths", "Screenshots on deaths", "Announce my pets", "Announce 99s and totals", "Clue completions",
        "Combat task completions", "Achievement diary completions", "PK loot"};

    /** Reads an upload switch by key without reflection; unknown keys read as off. Static, so RuneLite's proxy never sees it. */
    static boolean upload(IroncladEventsConfig config, String key)
    {
        switch (key)
        {
            case "autoDrops": return config.autoDrops();
            case "wallOfFameDrops": return config.wallOfFameDrops();
            case "screenshots": return config.screenshots();
            case "bankPrechecks": return config.bankPrechecks();
            case "liveProgress": return config.liveProgress();
            case "syncCollectionLog": return config.syncCollectionLog();
            case "syncCombatAchievements": return config.syncCombatAchievements();
            case "syncQuests": return config.syncQuests();
            case "syncLevels": return config.syncLevels();
            case "autoPbs": return config.autoPbs();
            case "pbScreenshots": return config.pbScreenshots();
            case "relayClanChat": return config.relayClanChat();
            case "autoDeaths": return config.autoDeaths();
            case "deathScreenshots": return config.deathScreenshots();
            case "sharePets": return config.sharePets();
            case "shareMilestones": return config.shareMilestones();
            case "shareClues": return config.shareClues();
            case "shareCombatTasks": return config.shareCombatTasks();
            case "shareDiaries": return config.shareDiaries();
            case "sendPkLoot": return config.sendPkLoot();
            default: return false;
        }
    }

    // ── 1. Connect ───────────────────────────────────────────────────────────
    @ConfigItem(section = connectionSection, keyName = "apiKey", name = "Plugin key", position = 0, secret = true,
        description = "Your personal key that links this plugin to your clan account. Easiest: turn on Connect, then press Link my account in the Ironclad side panel and it fills in itself. " + KEY_HELP + " Treat it like a password: never post it in chat or Discord.")
    default String apiKey() { return ""; }

    @ConfigItem(section = connectionSection, keyName = "connect", name = "Connect to ironcladrs.com", position = 1, warning = THIRD_PARTY_WARNING,
        description = "The one switch that lets this plugin talk to ironcladrs.com. Off: nothing is ever sent. On: it loads your events, team, tasks and claims into the side panel, and the uploads you pick in sections 2 to 9 can send. Each of those starts off.")
    default boolean connect() { return true; }

    @ConfigItem(section = connectionSection, keyName = "onlyAccounts", name = "Only these accounts", position = 2,
        description = "RuneScape names separated by commas. When filled in, the plugin sends nothing for any other account you log in with (for example your alts). Empty: every linked account.")
    default String onlyAccounts() { return ""; }

    // ── 2. Drops ─────────────────────────────────────────────────────────────
    @ConfigItem(section = dropsSection, keyName = "autoDrops", name = "Event tile drops", position = 0,
        description = "When a drop matches a tile or target in a live event you are rostered in, file the claim for you: items, source, kill count and time. Staff still review every claim. If this is off, matching drops are not sent at all.")
    default boolean autoDrops() { return true; }

    @ConfigItem(section = dropsSection, keyName = "wallOfFameDrops", name = "Wall of Fame drops", position = 1,
        description = "Send other drops worth at least the Wall of Fame minimum so they are posted on the clan's Wall of Fame. Off: event drops still file, but are never posted to the wall by this plugin.")
    default boolean wallOfFameDrops() { return true; }

    @ConfigItem(section = dropsSection, keyName = "screenshots", name = "Screenshots on drops", position = 2,
        description = "Add a screenshot of your whole game window, chat included, to each drop sent above, as proof for staff.")
    default boolean screenshots() { return true; }

    @Range(min = 0)
    @ConfigItem(section = dropsSection, keyName = "minimumWallValue", name = "Wall of Fame min (gp)", position = 3,
        description = "Only send Wall of Fame drops worth at least this much. 0 uses the clan's minimum. Event tile drops always send. Ignored while Use clan loot filters is on.")
    default int minimumWallValue() { return 0; }

    @ConfigItem(section = dropsSection, keyName = "ignoredLootItems", name = "Never send these items", position = 4,
        description = "Item names separated by commas, for example: Bones, Ashes. A drop made only of these items is not sent; a drop with anything else still sends in full. Careful: this can stop an event drop from filing. Ignored while Use clan loot filters is on.")
    default String ignoredLootItems() { return ""; }

    @ConfigItem(section = dropsSection, keyName = "ignoredLootSources", name = "Never send from these", position = 5,
        description = "Monster or activity names separated by commas, for example: Barrows, Tempoross. Nothing from them is sent. Careful: this can stop an event drop from filing. Ignored while Use clan loot filters is on.")
    default String ignoredLootSources() { return ""; }

    @ConfigItem(section = dropsSection, keyName = "alwaysSendItems", name = "Always send these items", position = 6,
        description = "Item names separated by commas. A drop containing any of these goes to the Wall of Fame even below the minimum value, like Dink's item allowlist. Never send lists still win.")
    default String alwaysSendItems() { return ""; }

    @Range(min = 0)
    @ConfigItem(section = dropsSection, keyName = "screenshotMinValue", name = "Screenshot min (gp)", position = 7,
        description = "Only add a screenshot to drops worth at least this much in total. 0: every drop that is sent gets one (while Screenshots on drops is on). Event tile drops always get one.")
    default int screenshotMinValue() { return 0; }

    @ConfigItem(section = dropsSection, keyName = "sendClueLoot", name = "Send clue casket loot", position = 8,
        description = "Treat rewards from clue caskets as drops (Wall of Fame and event tiles). Off: casket rewards are never sent.")
    default boolean sendClueLoot() { return true; }

    @Range(min = 25, max = 100)
    @ConfigItem(section = dropsSection, keyName = "screenshotScale", name = "Screenshot size (%)", position = 9,
        description = "Shrink every screenshot this plugin sends (drops, personal bests, deaths) to this percentage of your game window. Smaller uploads faster.")
    default int screenshotScale() { return 100; }

    @Range(min = 0)
    @ConfigItem(section = dropsSection, keyName = "rarityOverride", name = "Rarity override (1 in X)", position = 10,
        description = "A drop at least this rare goes to the Wall of Fame even below the minimum value. The odds come from ironcladrs.com (OSRSClans drop chances, the item's most common source). 0 turns it off.")
    default int rarityOverride() { return 1000; }

    @ConfigItem(section = dropsSection, keyName = "rarityAndValue", name = "Rare must meet min value", position = 11,
        description = "On: a rare drop still has to be worth the Wall of Fame minimum (Dink's \"Require both\").")
    default boolean rarityAndValue() { return false; }

    @ConfigItem(section = dropsSection, keyName = "sendPkLoot", name = "Send PK loot", position = 12,
        description = "Send loot from player kills to the Wall of Fame as \"Player kill\". The other player's name is never sent.")
    default boolean sendPkLoot() { return true; }

    // ── 3. Clan loot filters ─────────────────────────────────────────────────
    @ConfigItem(section = clanSection, keyName = "useClanSettings", name = "Use clan filters", position = 0,
        description = "Use the filters clan staff set on the website instead of your own (drop minimums and lists, screenshot rules, level and clue rules, ignored death regions, clan broadcasts). Your own values are kept for when you turn this off. To also turn on the uploads the clan recommends, press Enable clan defaults in the side panel.")
    default boolean useClanSettings() { return false; }

    // ── 4. Prechecks and progress ────────────────────────────────────────────
    @ConfigItem(section = eventsSection, keyName = "bankPrechecks", name = "Bank prechecks", position = 0,
        description = "When an event precheck asks how many of an item you own, open your bank and the side panel shows the exact counts it will send. Nothing is sent until you press Send. Only the asked items are counted, from bank, inventory and equipment.")
    default boolean bankPrechecks() { return true; }

    @ConfigItem(section = eventsSection, keyName = "liveProgress", name = "Live event progress", position = 1,
        description = "During events you are in, send XP and kill counts for the skills and bosses that event tracks, plus your logged-in minutes. The site shows it as waiting until Wise Old Man confirms it; scores only ever use Wise Old Man.")
    default boolean liveProgress() { return true; }

    // ── 5. Account sync ──────────────────────────────────────────────────────
    @ConfigItem(section = accountSection, keyName = "syncCollectionLog", name = "Collection log", position = 0,
        description = "Send which collection log slots you have and how many, when you open your log and when you get a new slot. Used by Collection Log Hunt, collection prechecks and your site profile. Open your log once so it can be read.")
    default boolean syncCollectionLog() { return true; }

    @ConfigItem(section = accountSection, keyName = "syncCombatAchievements", name = "Combat achievements", position = 1,
        description = "Send your combat achievement tier totals and the IDs of tasks you have completed, when they change. Used for your profile and events that ask for CA tiers.")
    default boolean syncCombatAchievements() { return true; }

    @ConfigItem(section = accountSection, keyName = "syncQuests", name = "Quests and diaries", position = 2,
        description = "Send each quest's state (not started, started, finished) and which achievement diary tiers you have done. Used to check event requirements.")
    default boolean syncQuests() { return true; }

    @ConfigItem(section = accountSection, keyName = "syncLevels", name = "Levels and account type", position = 3,
        description = "Send your skill levels and whether you are an ironman, hardcore or group ironman. Used for your profile and event requirements.")
    default boolean syncLevels() { return true; }

    // ── 6. Personal bests ────────────────────────────────────────────────────
    @ConfigItem(section = pbSection, keyName = "autoPbs", name = "Submit personal bests", position = 0,
        description = "When the game says you set a new personal best, send the time, team size, worn gear and combat levels to the clan PB ladder. Staff approve it before it shows.")
    default boolean autoPbs() { return true; }

    @ConfigItem(section = pbSection, keyName = "pbScreenshots", name = "Screenshots on PBs", position = 1,
        description = "Add a screenshot of your whole game window, chat included, to each personal best sent, as proof for staff.")
    default boolean pbScreenshots() { return true; }

    // ── 7. Clan chat ─────────────────────────────────────────────────────────
    @ConfigItem(section = chatSection, keyName = "relayClanChat", name = "Relay clan chat", position = 0,
        description = "Copy the Ironclad clan channel to the clan's Discord, as Clan Chat Webhooks does: members' messages (sender, rank, account type, text and time) and the clan's system broadcasts. Guest, friends, public and private chat are never sent.")
    default boolean relayClanChat() { return true; }

    @ConfigItem(section = chatSection, keyName = "relayBroadcasts", name = "Include clan broadcasts", position = 1,
        description = "Also relay the clan's system broadcasts (drops, levels, pets, joins and leaves). Off: only members' typed messages.")
    default boolean relayBroadcasts() { return true; }

    // ── 8. Deaths ────────────────────────────────────────────────────────────
    @ConfigItem(section = deathsSection, keyName = "autoDeaths", name = "Announce my deaths", position = 0,
        description = "Post your own deaths to the clan's Wall of Shame in Discord. No location, killer names or item losses are sent.")
    default boolean autoDeaths() { return true; }

    @ConfigItem(section = deathsSection, keyName = "deathScreenshots", name = "Screenshots on deaths", position = 1,
        description = "Also post a screenshot of your game window, chat included, with each death. Anyone in the clan Discord channel can see it.")
    default boolean deathScreenshots() { return true; }

    @ConfigItem(section = deathsSection, keyName = "ignoredDeathRegions", name = "Ignore deaths in regions", position = 2,
        description = "Region IDs separated by commas (shown by RuneLite's developer tools or the wiki). Deaths there are never announced, for example safe minigames like Castle Wars (9520) or the Inferno practice area.")
    default String ignoredDeathRegions() { return ""; }

    @ConfigItem(section = deathsSection, keyName = "ignoreSafeDeaths", name = "Ignore safe deaths", position = 3,
        description = "Don't announce deaths where nothing is lost: Castle Wars, Soul Wars, LMS, PvP Arena, NMZ, Pest Control, Barbarian Assault, your house, the Gauntlet and raids (for non-hardcore accounts) and more. Hardcore accounts always count.")
    default boolean ignoreSafeDeaths() { return true; }

    @ConfigItem(section = deathsSection, keyName = "safeDeathExceptions", name = "Safe deaths still shown", position = 4,
        description = "Safe deaths you still want announced, for example the Inferno or the Fight Caves.")
    default Set<SafeDeathException> safeDeathExceptions() { return EnumSet.of(SafeDeathException.INFERNO, SafeDeathException.FIGHT_CAVE, SafeDeathException.JAD_CHALLENGES); }

    // ── 9. Pets, levels, clues ───────────────────────────────────────────────
    @ConfigItem(section = activitySection, keyName = "sharePets", name = "Announce my pets", position = 0,
        description = "When you get a pet, send the game's pet message so the clan can celebrate it in Discord.")
    default boolean sharePets() { return true; }

    @ConfigItem(section = activitySection, keyName = "petDuplicates", name = "Include duplicate pets", position = 3,
        description = "Also announce a pet you already own (the game's \"would have been followed\" message).")
    default boolean petDuplicates() { return true; }

    @ConfigItem(section = activitySection, keyName = "shareMilestones", name = "Announce 99s and totals", position = 1,
        description = "Send your new 99s and total-level milestones for clan announcements. Levels you already had are never announced.")
    default boolean shareMilestones() { return true; }

    @Range(min = 1, max = 126)
    @ConfigItem(section = activitySection, keyName = "levelMinimum", name = "Minimum level", position = 4,
        description = "Only announce skill levels at or above this. 99 (the default) announces 99s only; 70 announces every level from 70 up (see Level interval).")
    default int levelMinimum() { return 99; }

    @Range(min = 1, max = 50)
    @ConfigItem(section = activitySection, keyName = "levelInterval", name = "Level interval", position = 5,
        description = "Above the minimum, announce every Nth level: 1 is every level, 5 is 75, 80, 85…")
    default int levelInterval() { return 1; }

    @ConfigItem(section = activitySection, keyName = "virtualLevels", name = "Virtual levels past 99", position = 6,
        description = "Count XP past 99 as levels 100 to 126 (virtual levels) for the rules above.")
    default boolean virtualLevels() { return false; }

    @Range(min = 25, max = 500)
    @ConfigItem(section = activitySection, keyName = "totalLevelInterval", name = "Total level interval", position = 7,
        description = "Announce your total level every this many levels, from 500 total up. 100 (the default) announces 1,500, 1,600…")
    default int totalLevelInterval() { return 100; }

    @ConfigItem(section = activitySection, keyName = "clueMinTier", name = "Lowest clue tier", position = 8,
        description = "Only share clue completions of this tier and above.")
    default ClueTier clueMinTier() { return ClueTier.BEGINNER; }

    @ConfigItem(section = activitySection, keyName = "shareCombatTasks", name = "Announce combat tasks", position = 9,
        description = "Announce each combat achievement task you complete, with its tier and name.")
    default boolean shareCombatTasks() { return true; }

    @ConfigItem(section = activitySection, keyName = "combatTaskMinTier", name = "Lowest combat task tier", position = 10,
        description = "Only announce combat tasks of this tier and above.")
    default CombatTaskTier combatTaskMinTier() { return CombatTaskTier.EASY; }

    @ConfigItem(section = activitySection, keyName = "shareDiaries", name = "Announce diaries", position = 11,
        description = "Announce each achievement diary tier you finish, with its area.")
    default boolean shareDiaries() { return true; }

    @ConfigItem(section = activitySection, keyName = "diaryMinTier", name = "Lowest diary tier", position = 12,
        description = "Only announce diaries of this tier and above.")
    default DiaryTier diaryMinTier() { return DiaryTier.EASY; }

    @ConfigItem(section = activitySection, keyName = "shareClues", name = "Share clue completions", position = 2,
        description = "Send the tier and your completion count each time you finish a clue, for clan clue stats.")
    default boolean shareClues() { return true; }

    // ── 10. Notices and panel ────────────────────────────────────────────────
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
