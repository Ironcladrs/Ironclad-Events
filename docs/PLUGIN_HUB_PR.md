# Draft Hub submission — do not submit yet

Ironclad Events is a side panel for Ironclad clan events on ironcladrs.com: team, revealed tasks, standings, priorities, prechecks and claim status. Every upload starts off with the exact third-party-server warning and goes only to https://ironcladrs.com. No event requires the plugin.

Optional local-player uploads: qualifying event/Wall of Fame drops and optional screenshots; counts for a small staff-named precheck item list, shown before sending; event-tracked XP/KC and logged-in event activity; collection log, combat achievements, levels, account type, quests and diaries; newly announced PBs with worn gear and optional proof. PBs require staff review; progress never scores until WOM confirms it. Additional independent opt-ins send own death/time (no position, killer names or loss values), optional publicly announced death screenshots, own pet notifications, level milestones and clue tier/count. All retain the exact server warning and default false. A separately isolated opt-in package relays only typed messages from the named clan channel, like Clan Chat Webhooks.

No port, file access, runtime loading, input injection, outgoing-chat modification, overlays, infoboxes or added game menu entries. HTTP code is in `api/ApiClient.java`, using the injected OkHttpClient/Gson asynchronously and refusing redirects. Uploads/retries/captures stay in memory. Account/world/consent changes cancel pending work.

Sensitive APIs: `CollectionLogReader` has the single documented allowed `menuAction(-1, InterfaceID.Collection.SEARCH_TOGGLE, MenuAction.CC_OP, 1, -1, "Search", null)` and the collection-log init `runScript`. Quest reads use RuneLite `Quest.getState(client)`, which invokes the quest-status script. Other players’ POH logs are excluded. Cache/CA script constants with no gameval equivalent are named and documented.

Readers/sanitization attribution and full BSD notices are included in NOTICE and adapted files. Public repository: https://github.com/Ironcladrs/Ironclad-Events; author Minusmoney; Java 11; build=standard; no main third-party dependencies.

Generated-by: Claude Code

## Submission gate

Re-read the current plugin-hub README, Plugin-Hub-Review, Rejected-or-Rolled-Back-Features and example-plugin AGENTS.md. Run the forbidden-API scan and Java 11 build. Verify all upload defaults/warnings and optimized PNGs. Record the owner’s manual acceptance results. Select the exact tested commit for one submission. Server switches may stay dark; unfinished client code must not be submitted.
