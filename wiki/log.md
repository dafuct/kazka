# Wiki Log

Append-only chronological record of operations on the wiki. Each entry begins with `## [YYYY-MM-DD] <op> | <description>` so it's parseable with `grep "^## \[" log.md | tail -N`.

Operations:
- `ingest` — a source was processed into the wiki.
- `query` — a question was answered against the wiki (typically only logged when the answer was filed back as synthesis).
- `lint` — a health check was run.
- `schema` — the schema was modified.
- `shard` — an index was sharded.

---

## [2026-05-21] lesson | filed bland-tale-output-needs-sampling-and-fewshot — story gen output flatness traced to missing sampling params + no in-language few-shot examples; both fixed

## [2026-05-22] lesson | filed tale-setup-contradicts-itself — tale opened with "Matviy lived in the house" then "Matviy went to the house"; added CONSISTENCY RULES block to storyteller prompt

## [2026-05-22] lesson | filed first-paragraph-indent-ukrainian-typography — CSS removed indent from first paragraph (English convention); Ukrainian typography indents every paragraph including the first

## [2026-05-22] lesson | filed editor-must-fix-invented-words-including-in-titles — editor left "Привидиний" in title; added invented-words rule + relaxed title carve-out to permit morphology fixes

## [2026-05-23] lesson | filed subscription-cancel-rules-differ-by-provider — captured per-provider cancel policy decided while building /settings page (Apple → App Store-only via 409 APPLE_MANAGED, Paddle/LiqPay/Monobank → local revoke; known gap: Paddle subscription-id not stored)

## [2026-05-25] feature | shipped Spec C — child profiles + recurring characters (keystone for D/E/F/G/H/I); migrations 010-013, new entities ChildProfile + Character + StoryCharacter, /api/children + /api/characters controllers, generation flow now requires childProfileId, async LLM-based character extraction with PENDING/RUNNING/DONE/FAILED/SKIPPED states, tier limits (free=1 profile/0 saved chars, paid=unlimited), entitlement-downgrade auto-archive listener, full frontend (ActiveChildPicker, settings pages, CharacterPicker on Home, ExtractedCharactersPanel on StoryDetailPage, archive filter chips, full UK+EN locales)

## [2026-05-25] lesson | candidate filed user-entitlements-fk-needs-cascade-on-user-delete — adding new ITs in Spec C exposed latent FK constraint failures in 15 test classes that called users.deleteAll() before entitlementRepo.deleteAll() (user_entitlements.user_id FK lacks ON DELETE CASCADE — masked until test ordering changed)

## [2026-05-25] feature | shipped Spec D — nightly bedtime ritual (paid-only); migration 014 bedtime_schedules (1:1 child_profile FK with ON DELETE CASCADE proactively applying the Spec C lesson), DST-correct NextRunCalculator with tests for Kyiv spring-forward (2026-03-29) + fall-back (2026-10-25) + diaspora New York, 5-minute @Scheduled sweep cron + @Async BedtimeWorker with 15-min back-off and 3-retry cap, MailService.sendHtml added for HTML email composition with XSS-safe escaping, EntitlementDowngradeListener extended to also disable bedtime schedules on downgrade (preserves local_time/timezone/themes), frontend Bedtime section on ChildProfileEditPage with TimezoneSelect + paid-tier teaser, full UK+EN i18n
