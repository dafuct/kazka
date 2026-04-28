# Kazka (Казкар) — Project Overview

**State:** Greenfield. Spec and implementation plan are written and committed; no code yet.

**What it is:** Full-stack Ukrainian/English fairy tale generator. Streams personalized bedtime stories via local Ollama (`gemma3:4b`) and illustrates them via local image model (`x/flux2-klein`, mac-only). Stories are persisted to Postgres and browseable in an archive (with edit/delete).

**Source of truth (read these first):**
- `docs/superpowers/specs/2026-04-27-kazkar-design.md` — design spec (approved)
- `docs/superpowers/plans/2026-04-27-kazkar.md` — 31-task TDD implementation plan

**Visual reference:** `docs/_ Standalone.html` (a self-contained mockup; assets extracted to `docs/extracted/` — 19 woff2 fonts + `template.html` showing the full CSS/markup).

**Planned layout:**
```
kazka/
├── backend/          # Spring Boot 4 service (port 8080)
├── frontend/         # React 19 + Vite SPA (port 5173)
├── docker-compose.yml
├── .env.example
└── README.md
```

**No cloud APIs.** All AI runs via Ollama on the host machine.

**Default locale:** Ukrainian (uk). English (en) is a runtime toggle in the nav.
