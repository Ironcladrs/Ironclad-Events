# Implementation and acceptance tracker

Implemented across the public plugin, bot and website checkouts on 2026-11-17. This replaces the earlier scaffold status. These are **development changes**, not a Plugin Hub release or production deployment. Automatic checks and the owner’s in-game acceptance are separate gates.

| Step | Development implementation |
| --- | --- |
| PC-1 / PS-1 | Java 11 plugin, final config keys/default warnings, immutable identity/session, fixed-host asynchronous API, memory-only outbox/captures, Setup, hashed/revocable keys, member settings, privacy notice and ingest routing. |
| PC-2 / PS-3 | Conditional polling/backoff, revealed event projections, own-team priorities, recommendations, local task search/icons, standings/claims/provisional progress, notices, event links and feature refresh. Shared task catalogue moved to the bot and reused by the website. |
| PC-3 / PS-2 | NPC/EVENT/PICKPOCKET collection, stacking and matching, screenshot uploads, shared Dink filing/de-duplication across adjacent minute buckets, later-proof enrichment, claim provenance and recent KC metadata. |
| PC-4 / PS-4 | Asked bank counts, explicit preview/send and per-event automatic consent, exact-match approval, picture capture/preview, staff collection evidence, captain responses, staff bank rule editor, audited evidence and deletion/teardown. |
| PC-5 / PS-5 | Event-only XP/KC and logged-in activity, provisional display, existing WOM-cycle confirmation/mismatch without extra fetches or telemetry scoring. |
| PC-6 / PS-6 | Own collection-log reader/new slot notifications, CA bitmaps/tier totals, account type, levels, quests/diaries, optional-part coalescing and one shared RuneProfile snapshot store. |
| PC-7 / PS-7 | New PB detection, own worn gear/levels/party size, optional screenshot, existing staff review, faster pending replacement, aliases, personal-only unknown bosses, ladder/profile equipment layout. |
| PC-8 / PS-8 | Isolated typed clan-chat batching/sanitization, clan filtering, cross-transmitter de-duplication, mention-safe staff relay, coverage and optional dark legacy-webhook fallback. |
| PS-9 | Member key/data management, staff feed coverage/transmitters, plugin-or-Dink event health, private proof gate, member export/erasure integration. |
| PC-9 | Java 11 build, unit/integration tests, browser scenarios, forbidden API/default/icon scans, review draft and manual test script prepared. Owner game acceptance and actual Hub submission remain open. |

## Choices and boundaries

- The owner explicitly skipped Discord `#development` preflight on 2026-11-17. Repo owner: the Ironcladrs GitHub organization; author: Minusmoney (never a real name). Re-read current RuneLite guidance before submission; reference approvals do not approve this plugin.
- All server feature flags start false. They are staff settings; this checkout does not turn production features on.
- The spec’s defaults apply: typed chat only, staff moderation channel, no database message log; screenshot-free PBs stay pending for staff. No real chat channel has been selected by this implementation.
- Own deaths, optional public death screenshots, pet notifications, level milestones and clue completions have independent off-by-default controls (owner decision, 2026-11-17). Appearance/model upload, later bank after-pictures and optional ClanSocket reception remain excluded.
- Account-hash strings, Settings help instead of unsupported config navigation, rate-bounded immediate KC reports and actual WOM integration paths are documented in [API_CONTRACT](docs/API_CONTRACT.md).
- RuneLite’s game/widget timing and parser coverage require real owner testing. All registered engines now project authoritative task status and current availability. Next steps combine owed evidence, available captain targets and current incomplete objectives; no parallel scorer is introduced.
- Private screenshots require the website gate for both local and R2 storage. R2 must not expose objects through a public bucket/domain. Plugin wall cards omit screenshot bytes; private queue evidence opens on the site. Existing Dink behavior remains as before.

## Acceptance

See [MANUAL_TESTS](docs/MANUAL_TESTS.md). Java 11 compilation and unit tests do not replace the in-game checks. Do not publish a Hub release until the owner has exercised core steps and any affected parser/widget flows on the exact commit. Server and website must be deployed deliberately before end-to-end testing against ironcladrs.com.

Observed automated results are recorded in [VALIDATION](docs/VALIDATION.md); the Git commit identifies the tested development snapshot. No agent has played the game or submitted a Hub PR.
