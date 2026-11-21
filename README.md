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

| Switch | Data and timing |
| --- | --- |
| Connect | Your RSN, RuneLite account hash, world types, plugin version and chosen switches when linking; identity accompanies subsequent requests. Reads your revealed event view while logged in. |
| File event drops automatically | Items, quantity, item price, source, time and recent KC when a drop matches a live rostered event or meets the clan’s Wall of Fame threshold. PvP loot is ignored. |
| Attach a screenshot | The whole game window, including chat, with qualifying drops and new personal bests. |
| Answer precheck questions from my bank | Counts for the specific staff-named items across bank, inventory and equipment. The panel shows exactly those counts before Send. Automatic sending for the rest of that event is a separate, initially unchecked choice. No bank or inventory snapshot leaves the computer. |
| Share live XP and kill counts | Current totals for the skills and bosses your live events track, plus logged-in seconds during your live rostered events. Progress is provisional until Wise Old Man confirms it; this feed never scores. |
| Sync my collection log… | Your collection log, combat achievements, levels, account type, group size, quests and diary completion. Collected when you open your own log; another player’s adventure log is ignored. Turn on the game’s collection-log notification to send new slots promptly. |
| Record my personal bests | A newly announced PB’s boss, time, party size, recent KC, eleven worn equipment slots and combat levels. Staff approval is required. Without a picture it is marked “no picture”; an unmatched boss remains personal and unranked. |
| Share my own deaths | Your own death and time, through the existing Wall of Shame path. No location, killer name, gear, inventory or loss-value calculation. Independent of automatic drops. |
| Attach screenshots to my deaths | Publish the whole game window, including chat, to the clan’s configured death-announcement channel. Separate from drop/PB screenshots. |
| Announce my new pets | Your own game pet notification. No clan broadcast or other player is inspected. |
| Announce my level milestones | Newly reached 99s and hundred-level total milestones (500+); logging in with an existing 99 does not announce it. |
| Share my clue completions | Own clue tier, completion total and time. Weekly reports are self-reported, never event scores. |
| Relay clan chat | Typed messages from the clan channel named by the server: sender, text and timestamp. No guest, friends, public or private chat; system broadcasts are excluded. Relayed to the configured staff moderation channel with mentions disabled. |

All thirteen switches are off by default and carry RuneLite’s full third-party-server warning. Picture prechecks are explicit **Capture → preview → Send** actions; they do not depend on the automatic-screenshot switch. Bank rules may approve exact matches automatically; mismatches go to staff. A staff-selected collection-log precheck can use a fresh synced snapshot as evidence for review.

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

All 21 settings are grouped into nine collapsible RuneLite sections, initially closed. Connection/key, clan defaults, drops, event progress/prechecks, account/PBs, clan chat, deaths, activity and local notices have separate groups. Existing keys are unchanged.

Loot filters cover a personal non-event Wall of Fame minimum, exact comma-separated source exclusions and drops containing only excluded item names. Exclusions can prevent automatic event submissions. The value minimum never blocks an event match. A mixed drop keeps its entire item list so Dink de-duplication remains exact. These are core filters, not the full Dink settings catalogue.

In the panel's Connection and uploads area, Preview clan settings shows the current revision and filter values. Use clan settings follows staff-managed filters on subsequent syncs. Use personal settings restores your saved filters. Before connecting, personal filters apply and the preset cannot be fetched. Staff edit the existing website Leadership settings group **Ironclad Events clan defaults**; numeric values are bounded to 2,147,483,647 GP and name lists to 500 characters.

Upload recommendations appear in the preview but are never applied. Enable each upload yourself through RuneLite's normal warning. Following clan settings cannot change Connect, your key, screenshots, chat relay, bank consent or any upload switch. Feature availability remains controlled separately by the server.
