--liquibase formatted sql

--changeset kazka:011-create-characters splitStatements:true endDelimiter:; runInTransaction:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'characters'
--comment Create characters library, scoped per child_profile. UNIQUE(child_profile_id, name) enables idempotent upsert on re-extraction.
--          Purpose:    Persistent recurring-cast for tales.
--          Risk:       low — additive table.
--          Reversible: yes — DROP TABLE characters.
--rollback DROP TABLE characters;

CREATE TABLE characters (
    id                VARCHAR(36)  NOT NULL PRIMARY KEY,
    child_profile_id  VARCHAR(36)  NOT NULL,
    name              VARCHAR(120) NOT NULL,
    kind              VARCHAR(20)  NOT NULL,
    description       TEXT         NOT NULL,
    traits            JSON         NOT NULL,
    first_story_id    VARCHAR(36)  NULL,
    last_used_at      DATETIME(3)  NULL,
    usage_count       INT UNSIGNED NOT NULL DEFAULT 0,
    archived_at       DATETIME(3)  NULL,
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_characters_profile     FOREIGN KEY (child_profile_id) REFERENCES child_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_characters_first_story FOREIGN KEY (first_story_id)   REFERENCES stories(id)        ON DELETE SET NULL,
    UNIQUE KEY uk_characters_profile_name (child_profile_id, name),
    INDEX idx_characters_profile_archived (child_profile_id, archived_at),
    INDEX idx_characters_profile_lastused (child_profile_id, last_used_at DESC)
);
