# Ironclad Events

RuneLite side panel for your Ironclad clan events on [ironcladrs.com](https://ironcladrs.com): team, standings, captain priorities, tasks, prechecks and claim status, with optional uploads of your own progress.

Development build. **Not published to Plugin Hub, deployed, or validated in game yet.** All upload switches start off. Server features also start off. Dink and ordinary picture submissions remain supported; no event requires this plugin.

## Connect

1. Sign in to the website with your verified Discord account. Open **Settings → My plugin data**, name a key and copy it while it is shown. Up to three active keys are allowed.
2. In RuneLite’s plugin settings, find **Ironclad Events**. Paste the key into **Your plugin key**, then enable **Connect to ironcladrs.com**. Read the third-party warning.
3. Enable only the upload features you want. The Setup checklist explains each one. An unavailable server feature stays off regardless of the local switch.
4. Log in on a standard world using an RSN linked to that Discord account. The panel explains an invalid key, unlinked account or unavailable connection.

The key is a credential. Never put it in a screenshot, URL, issue, chat or log. It is sent only as a Bearer header. Revoke a lost key on the website. The plugin cannot change the destination: `https://ironcladrs.com/ingest/plugin/v1`. Redirects are refused.

## What is sent

| Setting | What it sends, and when |
| --- | --- |
| Connect to ironcladrs.com | The one switch that lets this plugin talk to ironcladrs.com. Off: nothing is ever sent. On: it loads your events, team, tasks and claims into the side panel, and the uploads you pick in sections 2 to 9 can send. Each of those starts off. |
| Event tile drops | When a drop matches a tile or target in a live event you are rostered in, file the claim for you: items, source, kill count and time. Staff still review every claim. If this is off, matching drops are not sent at all. |
| Wall of Fame drops | Send other drops worth at least the Wall of Fame minimum so they are posted on the clan's Wall of Fame. Off: event drops still file, but are never posted to the wall by this plugin. |
| Screenshots on drops | Add a screenshot of your whole game window, chat included, to each drop sent above, as proof for staff. |
| Bank prechecks | When an event precheck asks how many of an item you own, open your bank and the side panel shows the exact counts it will send. Nothing is sent until you press Send. Only the asked items are counted, from bank, inventory and equipment. |
| Live event progress | During events you are in, send XP and kill counts for the skills and bosses that event tracks, plus your logged-in minutes. The site shows it as waiting until Wise Old Man confirms it; scores only ever use Wise Old Man. |
| Collection log | Send which collection log slots you have and how many, when you open your log and when you get a new slot. Used by Collection Log Hunt, collection prechecks and your site profile. Open your log once so it can be read. |
| Combat achievements | Send your combat achievement tier totals and the IDs of tasks you have completed, when they change. Used for your profile and events that ask for CA tiers. |
| Quests and diaries | Send each quest's state (not started, started, finished) and which achievement diary tiers you have done. Used to check event requirements. |
| Levels and account type | Send your skill levels and whether you are an ironman, hardcore or group ironman. Used for your profile and event requirements. |
| Submit personal bests | When the game says you set a new personal best, send the time, team size, worn gear and combat levels to the clan PB ladder. Staff approve it before it shows. |
| Screenshots on PBs | Add a screenshot of your whole game window, chat included, to each personal best sent, as proof for staff. |
| Relay clan chat | Copy the Ironclad clan channel to the clan's Discord, as Clan Chat Webhooks does: members' messages (sender, rank, account type, text and time) and the clan's system broadcasts. Guest, friends, public and private chat are never sent. |
| Announce my deaths | Post your own deaths to the clan's Wall of Shame in Discord. No location, killer names or item losses are sent. |
| Screenshots on deaths | Also post a screenshot of your game window, chat included, with each death. Anyone in the clan Discord channel can see it. |
| Announce my pets | When you get a pet, send the game's pet message so the clan can celebrate it in Discord. |
| Announce 99s and totals | Send your new 99s and total-level milestones for clan announcements. Levels you already had are never announced. |
| Share clue completions | Send the tier and your completion count each time you finish a clue, for clan clue stats. |
| Announce combat tasks | Announce each combat achievement task you complete, with its tier and name. |
| Announce diaries | Announce each achievement diary tier you finish, with its area. |
| Send PK loot | Send loot from player kills to the Wall of Fame as "Player kill". The other player's name is never sent. |

Connect and all twenty upload settings are off by default and carry RuneLite’s full third-party-server warning. After linking, the panel's **Use clan defaults** button turns on the uploads the clan recommends in one press, after one confirmation that names each one and repeats the warning. Picture prechecks are explicit **Capture → preview → Send** actions; they do not depend on the automatic-screenshot switch. Bank rules may approve exact matches automatically; mismatches go to staff. A staff-selected collection-log precheck can use a fresh synced snapshot as evidence for review.

**Screenshots include chat.** Event evidence is for the member and reviewing staff. Approved PB records, gear and proof can appear on the public ladder; profile visibility controls the member profile. Review your window before capturing a picture.

Never sent: position, region, world number, combat state, other players’ gear or locations, bank contents beyond asked counts, inventory contents, GE offers, wealth, friends, or other chat. No character model or appearance upload. Own deaths and their screenshots have separate opt-in controls; they work independently of automatic drops.

Chat notices, desktop notices and the side panel are local settings and default on; they send no data.

## Stop or delete sharing

Turn **Connect** off, disable the plugin, or revoke the key. Turning Connect off cancels requests and clears the in-memory upload queue; a best-effort session-close request tells the site coverage has stopped. Logging out stops reads and uploads. No non-standard profile sends data.

**Settings → My plugin data** lists key coverage and provides **Delete my bank answers**, **Delete my synced account data** and **Revoke key**. Deleting bank answers during a live event makes those prechecks owed again. Turn account sync off too if you do not want a later update to replace deleted data.

Pet/milestone/clue activity is kept for 30 days, shown as self-reported, and can be deleted in My plugin data. Death screenshots are delivered to Discord; the site does not store a death picture.

Bank evidence and provisional progress follow the existing 48-hour event teardown. Account snapshots keep only the latest state. Idempotency receipts last 24 hours; chat de-duplication hashes one hour, with no database message log. Screenshots follow the site’s existing image retention. Export and erasure include the new data.

## Development and validation

Use **Java 11**:

```sh
./gradlew build
./gradlew run
```

For a Jagex account follow [RuneLite’s login guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Only the owner tests in game; agents must not play or automate it. Follow [the manual test guide](docs/MANUAL_TESTS.md) before a Hub submission.

Main code uses RuneLite-provided dependencies only, injected OkHttp/Gson, asynchronous HTTPS, memory-only uploads and screenshots, one managed worker, one panel and no overlays or added game menu entries. The one sensitive `menuAction` is the documented collection-log search toggle. See [review notes](docs/PLUGIN_HUB_PR.md), [implementation status](IMPLEMENTATION.md), [wire contract](docs/API_CONTRACT.md), [LICENSE](LICENSE) and [NOTICE](NOTICE).

The owner skipped the Discord `#development` preflight on 2026-11-17. Current Hub guidance was read that day; it must be read again before submission. Reference plugin approval does not approve this plugin.

Future event types can use the same server-defined panel fields without a client update. Unfamiliar view versions or layouts show the event and its website link while pausing automatic event reporting. New kinds of game observation still require a plugin update. See [the compatibility contract](docs/API_CONTRACT.md).

### Settings and clan defaults

The 48 settings are grouped into ten collapsible RuneLite sections: Connect, Drops, Clan filters, Prechecks and progress, Account sync, Personal bests, Clan chat, Deaths, Pets, levels, clues, and Notices and panel. Existing keys are unchanged.

Loot filters cover a personal non-event Wall of Fame minimum, exact comma-separated source exclusions and drops containing only excluded item names. Exclusions can prevent automatic event submissions. The value minimum never blocks an event match. A mixed drop keeps its entire item list so Dink de-duplication remains exact. These are core filters, not the full Dink settings catalogue.

In the panel's Connection and uploads area, Preview clan settings shows the current revision and filter values. Use clan settings follows staff-managed filters on subsequent syncs. Use personal settings restores your saved filters. Before connecting, personal filters apply and the preset cannot be fetched. Staff edit the existing website Leadership settings group **Ironclad Events clan defaults**; numeric values are bounded to 2,147,483,647 GP and name lists to 500 characters.

Upload recommendations appear in the preview but are never applied. Enable each upload yourself through RuneLite's normal warning. Following clan settings cannot change Connect, your key, screenshots, chat relay, bank consent or any upload switch. Feature availability remains controlled separately by the server.
