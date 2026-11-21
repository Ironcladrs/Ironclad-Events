package com.ironcladrs.events;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(IroncladEventsConfig.GROUP)
public interface IroncladEventsConfig extends Config
{
    String GROUP = "ironcladevents";
    String THIRD_PARTY_WARNING = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers";

    @ConfigSection(name = "Connection", description = "Connection settings.", position = 0, closedByDefault = true)
    String connectionSection = "connection";

    @ConfigSection(name = "Clan settings", description = "Clan settings settings.", position = 1, closedByDefault = true)
    String clanSection = "clan";

    @ConfigSection(name = "Drops and screenshots", description = "Drops and screenshots settings.", position = 2, closedByDefault = true)
    String dropsSection = "drops";

    @ConfigSection(name = "Prechecks and progress", description = "Prechecks and progress settings.", position = 3, closedByDefault = true)
    String eventsSection = "events";

    @ConfigSection(name = "Account and personal bests", description = "Account and personal bests settings.", position = 4, closedByDefault = true)
    String accountSection = "account";

    @ConfigSection(name = "Clan chat", description = "Clan chat settings.", position = 5, closedByDefault = true)
    String chatSection = "chat";

    @ConfigSection(name = "Deaths", description = "Deaths settings.", position = 6, closedByDefault = true)
    String deathsSection = "deaths";

    @ConfigSection(name = "Pets, levels and clues", description = "Pets, levels and clues settings.", position = 7, closedByDefault = true)
    String activitySection = "activity";

    @ConfigSection(name = "Notifications and panel", description = "Notifications and panel settings.", position = 8, closedByDefault = true)
    String noticesSection = "notices";

    @ConfigItem(section = connectionSection, keyName = "connect", name = "Connect to ironcladrs.com", description = "Allow this plugin to connect to the clan website.", position = 2, warning = THIRD_PARTY_WARNING)
    default boolean connect()
    {
        return false;
    }

    @ConfigItem(section = dropsSection, keyName = "autoDrops", name = "File event drops automatically", description = "Send qualifying event drops and Wall of Fame drops.", position = 3, warning = THIRD_PARTY_WARNING)
    default boolean autoDrops()
    {
        return false;
    }

    @ConfigItem(section = dropsSection, keyName = "screenshots", name = "Attach a screenshot to drops and personal bests", description = "Screenshots include the whole game window, including chat.", position = 4, warning = THIRD_PARTY_WARNING)
    default boolean screenshots()
    {
        return false;
    }

    @ConfigItem(section = eventsSection, keyName = "bankPrechecks", name = "Answer precheck questions from my bank", description = "Send counts for asked items only, after showing them to you.", position = 5, warning = THIRD_PARTY_WARNING)
    default boolean bankPrechecks()
    {
        return false;
    }

    @ConfigItem(section = eventsSection, keyName = "liveProgress", name = "Share live XP and kill counts during events", description = "Send event-tracked skills and bosses, plus logged-in minutes in your live events.", position = 6, warning = THIRD_PARTY_WARNING)
    default boolean liveProgress()
    {
        return false;
    }

    @ConfigItem(section = accountSection, keyName = "syncAccount", name = "Sync my collection log, combat achievements, quests, diaries and levels", description = "Send your own account snapshot.", position = 7, warning = THIRD_PARTY_WARNING)
    default boolean syncAccount()
    {
        return false;
    }

    @ConfigItem(section = accountSection, keyName = "autoPbs", name = "Record my personal bests (time and worn gear)", description = "Send new personal bests with worn gear and combat levels.", position = 8, warning = THIRD_PARTY_WARNING)
    default boolean autoPbs()
    {
        return false;
    }

    @ConfigItem(section = chatSection, keyName = "relayClanChat", name = "Relay clan chat to the clan's Discord", description = "Send messages from the clan channel only.", position = 9, warning = THIRD_PARTY_WARNING)
    default boolean relayClanChat()
    {
        return false;
    }

    @ConfigItem(section = deathsSection, keyName = "autoDeaths", name = "Share my own deaths", description = "Announce your own deaths to the clan Wall of Shame. No location, killer names or item-loss values.", position = 13, warning = THIRD_PARTY_WARNING)
    default boolean autoDeaths() { return false; }

    @ConfigItem(section = deathsSection, keyName = "deathScreenshots", name = "Attach screenshots to my deaths", description = "Publish the game window, including chat, with your death in the clan's configured Discord channel.", position = 14, warning = THIRD_PARTY_WARNING)
    default boolean deathScreenshots() { return false; }

    @ConfigItem(section = activitySection, keyName = "sharePets", name = "Announce my new pets", description = "Send your own pet notification for clan announcements.", position = 15, warning = THIRD_PARTY_WARNING)
    default boolean sharePets() { return false; }

    @ConfigItem(section = activitySection, keyName = "shareMilestones", name = "Announce my level milestones", description = "Send your own 99s and total-level milestones.", position = 16, warning = THIRD_PARTY_WARNING)
    default boolean shareMilestones() { return false; }

    @ConfigItem(section = activitySection, keyName = "shareClues", name = "Share my clue completions", description = "Send your own clue tier and completion count for clan statistics.", position = 17, warning = THIRD_PARTY_WARNING)
    default boolean shareClues() { return false; }

    @ConfigItem(section = connectionSection, keyName = "apiKey", name = "Your plugin key", description = "Get a key from the site's Plugins page. Never share it.", position = 1, secret = true)
    default String apiKey() { return ""; }

    @ConfigItem(section = noticesSection, keyName = "chatNotices", name = "Tell me in chat when something is filed or decided", description = "Local setting; does not send data.", position = 10)
    default boolean chatNotices() { return true; }

    @ConfigItem(section = noticesSection, keyName = "desktopNotices", name = "Notify me about event and claim updates", description = "Local setting; does not send data.", position = 11)
    default boolean desktopNotices() { return true; }

    @ConfigItem(section = noticesSection, keyName = "showPanel", name = "Show the side panel", description = "Local setting; does not send data.", position = 12)
    default boolean showPanel() { return true; }

    @ConfigItem(section = clanSection, keyName = "useClanSettings", name = "Use clan settings", description = "Follow the clan's loot filters after connecting. Upload switches remain your own choices.", position = 0)
    default boolean useClanSettings() { return false; }

    @Range(min = 0)
    @ConfigItem(section = dropsSection, keyName = "minimumWallValue", name = "Minimum Wall of Fame value", description = "GP threshold for non-event drops. 0 uses the server minimum. Event matches are unaffected. Overridden while using clan settings.", position = 20)
    default int minimumWallValue() { return 0; }

    @ConfigItem(section = dropsSection, keyName = "ignoredLootItems", name = "Ignore drops containing only these items", description = "Comma-separated exact item names. Mixed drops retain the complete item list. Can suppress event submissions. Overridden while using clan settings.", position = 21)
    default String ignoredLootItems() { return ""; }

    @ConfigItem(section = dropsSection, keyName = "ignoredLootSources", name = "Ignore loot from these sources", description = "Comma-separated exact source names. Can suppress event submissions. Overridden while using clan settings.", position = 22)
    default String ignoredLootSources() { return ""; }
}
