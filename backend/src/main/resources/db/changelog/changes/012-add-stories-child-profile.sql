--liquibase formatted sql

--changeset kazka:012-add-stories-child-profile splitStatements:true endDelimiter:; runInTransaction:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stories' AND column_name = 'child_profile_id'
--comment Add child_profile_id (nullable for back-compat) + extraction_status to stories. FK + referencing index in same migration per global rules.
--          Purpose:    Attribute tales to a child profile; track async character-extraction pipeline state.
--          Risk:       low — additive columns, nullable FK and DEFAULT for NOT NULL column.
--          Reversible: yes — drop columns + drop FK + drop index.
--rollback ALTER TABLE stories DROP FOREIGN KEY fk_stories_child_profile;
--rollback ALTER TABLE stories DROP INDEX idx_stories_child_profile_created;
--rollback ALTER TABLE stories DROP COLUMN child_profile_id, DROP COLUMN extraction_status;

ALTER TABLE stories
    ADD COLUMN child_profile_id  VARCHAR(36) NULL,
    ADD COLUMN extraction_status VARCHAR(20) NOT NULL DEFAULT 'pending';

ALTER TABLE stories
    ADD CONSTRAINT fk_stories_child_profile
        FOREIGN KEY (child_profile_id) REFERENCES child_profiles(id) ON DELETE SET NULL;

CREATE INDEX idx_stories_child_profile_created
    ON stories (child_profile_id, created_at DESC);
