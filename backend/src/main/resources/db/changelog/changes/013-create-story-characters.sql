--liquibase formatted sql

--changeset kazka:013-create-story-characters splitStatements:true endDelimiter:; runInTransaction:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'story_characters'
--comment Many-to-many join between stories and characters with a role hint.
--          Purpose:    Track which characters appear in which tales (for archive display + character usage stats).
--          Risk:       low — additive join table.
--          Reversible: yes — DROP TABLE story_characters.
--rollback DROP TABLE story_characters;

CREATE TABLE story_characters (
    story_id      VARCHAR(36)  NOT NULL,
    character_id  VARCHAR(36)  NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    PRIMARY KEY (story_id, character_id),
    CONSTRAINT fk_sc_story     FOREIGN KEY (story_id)     REFERENCES stories(id)    ON DELETE CASCADE,
    CONSTRAINT fk_sc_character FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE,
    INDEX idx_sc_character (character_id)
);
