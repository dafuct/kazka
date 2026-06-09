--liquibase formatted sql

--changeset kazka:026-add-story-narration
--comment: Cached read-aloud narration (Gemini 2.5 Flash TTS). narration_status mirrors illustration_status; narration_key points at the cached WAV in object storage.
-- Migration: 026-add-story-narration
-- Purpose: lazy, cached neural narration for «Читати вголос» — status + storage key on stories.
-- Risk: low — additive, nullable/defaulted columns, no backfill, no index needed (lookup is by PK).
-- Reversible: yes — DROP COLUMN narration_status, narration_key.
ALTER TABLE stories
    ADD COLUMN narration_status VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    ADD COLUMN narration_key    VARCHAR(255) NULL;
--rollback ALTER TABLE stories DROP COLUMN narration_status, DROP COLUMN narration_key;
