# v1 wire and service notes

The bot owns `https://ironcladrs.com/ingest/plugin/v1`. Website management routes call bot services; they do not duplicate key, claim, precheck, snapshot or PB logic. Caddy sends `/ingest/*` to the bot. No deployment is performed by this repository.

Every request binds a Bearer key to a verified linked RSN, exact account hash and allowed standard world types. The server takes stored identity from that link. `accountHash` is a **decimal string**: Java’s 64-bit account hash must not lose precision in JavaScript JSON. It never goes in a credential URL. POSTs have UUID-v4 `idempotencyKey`; 24-hour receipts bind UUID, route and body hash. Retries reuse the UUID. In-flight repeats return 503; conflicting reuse is 400. Only 429/5xx/transport failures retry. Duplicates are successful receipts, not second claims.

`POST /hello` includes version and enabled switches. It returns member, clan name, minVersion, feature flags, polling/size budgets and wall threshold. Every server flag defaults false. `/sync` returns revealed rostered events, tasks, public standings, own team priorities/recommendations, owed prechecks, latest claims, ladder aliases, notices and provisional progress. It additionally carries fresh `features`; a switch change affects the ETag and takes effect without restarting. ETags/304 are honored; logout stops polling. Current-account event metrics exclude a rostered alt belonging to the same Discord member.

Writes are the spec endpoints: multipart `/loot`, `/precheck` and `/pb` (JSON `payload`, optional JPEG `screenshot`); JSON `/precheck/attest`, `/recommendations/{id}/response`, `/progress`, `/account` and `/chat`. Explicit picture prechecks require a screenshot regardless of the automatic screenshot switch. `/session` DELETE closes coverage best effort.

Loot uses ServerNpcLoot and RuneLite EVENT/PICKPOCKET loot, stacks item IDs, and passes normalized drops into Dink’s shared handler. Dink and plugin claim minute ±1 buckets in the existing `dink_dedup` receipt. The first files; a later picture enriches empty proof only. Recent KC is attached as nullable proof metadata on the existing claim rows, including when a second feed supplies it later. The existing claim engines and review policies remain authoritative.

Bank rules are staff configured on existing precheck types before the event starts. Only exact asked item keys and the three named sources are accepted. Counts are shown locally first. Exact expected answers use existing precheck approval; mismatches stay pending. Staff may choose collection evidence instead: a fresh (≤10-minute) snapshot files obtained/missing values (1/0) into the same precheck for review, never bank ownership. Pictures retain the same staff workflow and optional member note.

`/account` permits optional parts. Full collection reads set `full:true`; new-slot updates set `full:false,newItem:true` and merge without erasing other slots. The existing RuneProfile snapshot store is still the sole reader. CA task IDs are bitfield completion IDs; quest values include the game’s name and state; diary values are four completion flags. No account payload includes gear.

PBs contain eleven optional worn slots, combat levels, boss, time, team size and recent KC. The existing ladder stores pending submissions. A faster pending time replaces the prior pending time; an unknown boss goes into a personal-only, inactive bucket which cannot rank. No-picture plugin submissions are allowed for staff judgment. Approved PB proof/gear can be public.

`/progress` adds `activeSeconds:[{eventId,seconds}]` to event-scoped XP/KC totals. Only logged-in live-event time is counted, capped per report and idempotent per UUID. It sends neither world number nor position. Client immediate KC reports honor the 300-second endpoint budget: intervening kills coalesce. Provisional reports never score. The actual existing WOM cycle is `ironcladrs/server/womLive.js` → `bulkGains.js`; confirmation consumes its already-fetched snapshots, without a second WOM caller or additional request budget.

Chat adds an explicit `clanName` field; `clanTitle` is reserved for the sender’s title, never used as party/player inspection. Only typed clan messages (`systemMessageType:1`) are accepted. Delivery is 10 lines/5 seconds, mentions disabled, no database text log. The separate, dark fallback webhook accepts Clan Chat Webhooks’ NORMAL/epoch-seconds format. That legacy format has no clan-name field, so its upstream configuration must filter to Ironclad.

Plugin screenshots use `plugin-UUID` upload names and ownership metadata. An authorization gate precedes both local and R2 upload reads. Event proof is owner/staff only; approved PB proof may be public. Staff reads are audited. Plugin wall announcements exclude screenshot bytes; queue cards link to authenticated evidence rather than embedding a publicly readable image. Existing Dink proof behavior is unchanged. **R2 must remain private**, with reads through the site gate. Existing image retention/export/erasure cover these objects and metadata.

The bot’s existing 48-hour event teardown clears bank counts and their duplicated typed field/tile values, provisional progress and activity. Decisions remain. Member deletion also clears private proof metadata, snapshot data, bank details, keys, notices and responses. No new scoring table reads telemetry.

## Intentional compatibility choices

- RuneLite 1.13.6 exposes no supported API to open a particular plugin’s config page. The Setup card uses **Settings help** with instructions for the RuneLite configuration cog; no reflection or UI input injection.
- Some collection/CA enum/script IDs have no gameval aliases. They are named constants with source notes. Diary reads use gameval except three Karamja flags without matching aliases.
- Own deaths now have a separate opt-in feed and screenshot control, independent of drops. Appearance/model upload and optional ClanSocket relay are outside this release.
- Future card rendering, richer eligibility suggestions and the roadmap’s next-step engine consume the same stores when available; no duplicate scorer or rules engine is introduced here.

## 2026-11-17 additions

`POST /death` is multipart with the same identity/UUID envelope. Its payload has only `at`; optional `screenshot` uses independent `autoDeaths`/`deathScreenshots` consent. Server flag `deaths` defaults off. Dink and plugin pair nearest unpaired deaths within 90 seconds in the existing receipt store, independently of loss values. Distinct rapid plugin deaths use distinct UUIDs; Dink retains its legacy same-loss/minute retry identity. Simultaneous delivery returns retryable 503 until the first feed finishes. Own-death reports do not need automatic drops. Since loss values are never collected, the Dink GP floor applies only to value-bearing Dink reports; the plugin death feature explicitly accepts unknown-value deaths.

`POST /activity` accepts up to 20 typed own events: `pet`, `level` (skill/99), `total` (hundred-level milestone) or `clue` (tier/count), each with `at`. Server flags `pets`, `milestones` and `clues` default off. Local controls `sharePets`, `shareMilestones` and `shareClues` are independent. Raw chat, location, other player information and gear are not accepted into storage. Pet and level announcements reuse Dink’s channel and cross-feed receipt keys; explicit plugin flags are independent of Dink-only notifier toggles. Weekly activity is informational, never a scorer input. History is deleted after 30 days by the existing cleanup sweep; members can delete it earlier.

`eventTaskProgress` formats authoritative read views for every registered engine. It does not calculate a new score. Paths keys come from the engine catalogue; maze views retain fog filtering; positional objectives say whether they are current. Repeating rules/pages remain repeatable. Next steps prefer owed evidence, then available captain targets, then current incomplete objectives. A WOM goal remains unconfirmed until the ordinary goal evaluator approves it.

### Event view compatibility

Each `/sync.events[]` carries `view: {version: 1, mode: "standard" | "generic", message: "…"}`. Identity, name, schedule and same-origin `url` form the stable fallback envelope. Additive fields and new engine names using existing semantics keep version 1. The client has no engine-name allow-list. Missing `view` supports original v1 servers; malformed/unknown versions and modes fail closed to a website handoff, with event matching, prechecks, captain actions and progress disabled. Independent Wall of Fame matching and other opt-in personal features retain their own controls.

The bot's `pluginEventContract.EVENT_VIEWS` explicitly registers every `EVENT_TYPES` value. New engines choose a full standard adapter or an intentional generic website handoff. Unknown engines use the same generic fallback before invoking engine-specific readers. Existing account-roster, secret, practice and draft filters remain mandatory. A new game observation needs a client change; this contract transports data, never executable rules or actions.

### Clan recommendations and loot filters

`/hello` and `/sync` include `clanPreset: {version:1,name,revision,filters:{minimumWallValue,ignoredLootItems,ignoredLootSources},recommended:{...}}`. Revision hashes cover the allow-listed data, so a staff edit changes the sync ETag. The existing audited Leadership settings registry owns these values; neither arbitrary config keys nor credentials are accepted. Recommended booleans are display-only. All feature switches and normal upload consent stay independent. The local `useClanSettings` choice follows filters only; disabling it restores preserved personal values. New client keys: `useClanSettings` (false), `minimumWallValue` (0), `ignoredLootItems` (empty), `ignoredLootSources` (empty). All original keys are retained.
