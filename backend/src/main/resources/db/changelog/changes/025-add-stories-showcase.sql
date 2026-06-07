--liquibase formatted sql

-- Migration: 025-add-stories-showcase
-- Purpose: add a boolean `showcase` flag to `stories` so admins can mark curated
--          tales as public samples shown to unregistered visitors.
-- Risk: low — additive NOT NULL column with a DEFAULT; no data backfill required.
-- Reversible: yes — DROP COLUMN stories.showcase (no data depends on it yet).

--changeset kazka:025-add-stories-showcase
ALTER TABLE stories ADD COLUMN showcase BOOLEAN NOT NULL DEFAULT FALSE;
--rollback ALTER TABLE stories DROP COLUMN showcase;
