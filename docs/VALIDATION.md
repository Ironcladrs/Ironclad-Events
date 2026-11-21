# Automated validation — 2026-11-17

Development validation only. No agent logged into or played RuneScape. No production deploy, real Discord relay or Plugin Hub submission occurred.

| Check | Observed result |
| --- | --- |
| Java 11 build | Temurin 11.0.33, Gradle 8.10: `build` successful; 54 JUnit tests, zero failures. |
| Bot full suite | 914 main + 107 focused tests, zero failures (1,021 total). |
| Website full suite | 540 main + 112 focused tests, zero failures (652 total). Tests use fresh isolated databases; fixture-dependent legacy cases may skip when no real roster exists. |
| Browser | Four tests passed: actual isolated key create/hide/revoke, owner-only activity deletion and PB equipment/privacy, desktop and phone. The two public item sprites are fixture assets; no game session was used. |
| Stress rehearsal | 127 fixture players/9 teams/100 tiles; 800 messages → 650 claims and 650 concurrent approvals; 20 backfill claims, zero replay duplicates; SQLITE_BUSY=0; zero fixture residue. Synthetic-item price directory supplied locally, not a live Wiki dependency. |
| Migration repeatability | Fresh schema opened repeatedly; `PRAGMA integrity_check` returned `ok`; `PRAGMA foreign_key_check` returned no rows. |
| Review API scan | Forbidden API pattern: no hits. Exactly one `menuAction`, in CollectionLogReader. No `com.example` in main code. Thirteen false-default upload switches with the exact warning. |
| Assets | Root icon and panel icon are real 32×32 PNGs. LICENSE/NOTICE include adapted-code licenses and original WikiSync credit. |
| Website build | Successful. Main chunk 439.26 KB / 126.71 KB gzip. Unchanged baseline 438.52 KB / 126.50 KB gzip: +0.21 KB gzip; the pre-existing main bundle is already above the house’s approximate target. |
| Design check | Code scan: no findings. Rendered privacy scan: eight findings and one advisory both before and after, all in the pre-existing legal-page heading/body. New plugin prose has a readable measure. No new findings. |
| Layout review | Before/after screenshots inspected at 375×844 and 1440×900; no settings horizontal overflow. PB worn-slot images load in the browser fixture. |
| Whitespace | `git diff --check` clean in all three repositories. |

The full suites cover shared claim routing, cross-minute and two-feed receipts, later proof enrichment, bank approval/mismatch/teardown, collection evidence, snapshot merging/source precedence, provisional confirmation/mismatch, PB review/pending replacement/ambiguous aliases, chat de-duplication, owner-only management and private proof access. Client tests cover opt-in defaults, standard-world identity, account-transition buffer clearing, upload retries, redirect refusal, API mapping, matching/collection, bank folding, PB/KC parsing, other-player log exclusion, screenshot bounds/timeouts, sync/backoff/feature changes, UI URL/HTML guards and immutable list caps.

Review screenshots and raw local logs are retained under `/workspace/ironclad-review` and `/tmp/ironclad-*.log` in this workspace. They contain synthetic test identities and no production keys. The owner’s remaining acceptance steps are in [MANUAL_TESTS](MANUAL_TESTS.md). A successful build does not establish game-widget timing or complete chat-parser coverage.

Owner amendment coverage: local-player deaths with independent screenshot consent, Dink/plugin death pairing without collapsing distinct rapid plugin deaths, quiet login baselines for milestones, typed clue/pet activity, 30-day expiry and member erasure, and revealed progress projections for all twelve event engines. Activity never enters event scoring.

Future-event compatibility (2026-11-17): current and future-named engines share the v1 standard view; unknown engine fallback is tested through the real sync composer alongside a standard event. Registration coverage loops over EVENT_TYPES. Client tests cover unsupported versions/modes, malformed view metadata, sanitised action fields and fallback Swing rendering on the EDT. Fallback does not defeat account-roster or secret/practice/draft gates. No new website code or database schema was needed.

Clan settings/filter validation (2026-11-18): 51 Java tests pass, including preservation of personal filters, unchanged upload consent, live clan filter values, exact exclusions, mixed-item payload preservation, event matches below the Wall minimum, and the local-only return-to-personal action. All 21 config items belong to nine initially collapsed sections. Full bot suite passes 914 + 107 tests, including audited staff edits, changed sync ETags, fixed preset bounds and unchanged server feature flags. Main code retains thirteen false-default upload controls and warnings; the additional false-default preset switch is local only. No new game session or production deploy occurred.

Scrolling regression (2026-11-18): the owner's long-event screenshot exposed the disabled RuneLite scroll wrapper. `PanelScrollTest` failed first because the actual sidebar had no scroll container, then passed after enabling RuneLite's standard wrapper. At 242×500 with 100 tasks it verifies top Sync visibility, no horizontal scrollbar, full footer reachability and both Sync actions. Full Java 11 build: 52 tests, zero failures. Owner has confirmed connection and rostered-event display; scroll-wheel behaviour in the real client remains for owner confirmation.

Owner setup screenshot follow-up: setup switches use explicit On/Off words, completed tasks use Done, and server apostrophes are rendered as ASCII in wrapped text because the owner's RuneLite font did not render the former glyphs. Full Java build: 53 tests pass.

Precheck preview follow-up (2026-11-18): View full size opens the original capture in a bounded scroll pane without sending it. A Swing regression verifies a 1600×1000 capture retains its pixels and scrolls in both directions. Full Java 11 build: 54 tests pass. Owner confirmed plugin-only drop screenshots and approval status/chat/desktop notifications. Bank-count testing is not yet accepted: the observed precheck was configured for picture evidence, and bank rules require item-linked fields plus explicit staff enablement before event start.
