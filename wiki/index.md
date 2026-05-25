# Wiki Index

The catalog of all pages in this wiki. Each entry: a wikilink to the page and a one-line summary. The LLM reads this first when answering queries to identify candidate pages.

Keep summaries tight — one line each. The index is engineered to be cheap to read; a fat index defeats its purpose.

When this file exceeds ~300 lines or the wiki passes ~150 pages, shard into `wiki/indexes/<type>.md` and replace this file with a directory of shards. See the `scaling-playbook.md` reference in the `llm-wiki` skill for the migration procedure.

---

## Lessons

- [[bland-tale-output-needs-sampling-and-fewshot]] — flat MamayLM output was sampling/few-shot, not the model; tune both before fine-tuning
- [[tale-setup-contradicts-itself]] — tale openings contradicted their own setup; fixed with explicit CONSISTENCY RULES block in the storyteller prompt
- [[first-paragraph-indent-ukrainian-typography]] — first paragraph of every tale rendered flush-left (English convention); Ukrainian typography indents every paragraph
- [[editor-must-fix-invented-words-including-in-titles]] — editor left invented Ukrainian words ("Привидиний") in titles; carve-out was too broad and rule list missed neologisms
- [[subscription-cancel-rules-differ-by-provider]] — generic "Cancel subscription" is wrong; Apple is App Store-managed, Paddle needs subscription-id+API call, LiqPay/Monobank are one-off today
- [[user-entitlements-fk-needs-cascade-on-user-delete]] — user_entitlements.user_id FK lacks ON DELETE CASCADE; test ordering hid this until Spec C ITs reshuffled execution and broke 15 tests at once

## Sources

(populated as sources are ingested)

## Entities

(populated as entity pages are created)

## Concepts

(populated as concept pages are created)

## Synthesis

(populated as query answers are filed back)
