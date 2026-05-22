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
