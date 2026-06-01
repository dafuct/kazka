--liquibase formatted sql

-- Migration: V020__Comics_panels
-- Purpose: replace single-image illustration columns with a per-panel table; wipe legacy tales so archive is consistent from day one
-- Risk: high — destructive DELETE of all existing stories and child rows
-- Reversible: no — old story data is irrecoverable; orphaned R2 objects cleaned by scripts/cleanup-old-illustrations.sh

--changeset kazka:020-wipe-existing-stories
-- Liquibase's DATABASECHANGELOG already tracks per-changeset execution, so this
-- DELETE only ever runs once per environment. No precondition needed.
DELETE FROM stories;
--rollback empty

--changeset kazka:020-create-story-panels
CREATE TABLE story_panels (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    story_id        VARCHAR(36)  NOT NULL,
    panel_index     INT          NOT NULL,
    image_path      VARCHAR(512) NOT NULL,
    scene_prompt    TEXT         NOT NULL,
    narration       TEXT         NOT NULL,
    dialog_json     JSON         NULL,
    aspect          VARCHAR(16)  NOT NULL,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_story_panels_story
        FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    UNIQUE KEY uk_story_panels (story_id, panel_index)
);
--rollback DROP TABLE story_panels;

--changeset kazka:020-drop-stories-illustration-columns
ALTER TABLE stories
    DROP COLUMN illustration_path_light,
    DROP COLUMN illustration_path_dark;
--rollback ALTER TABLE stories ADD COLUMN illustration_path_light VARCHAR(500) NULL, ADD COLUMN illustration_path_dark VARCHAR(500) NULL;
