--liquibase formatted sql

--changeset kazka:017-add-stories-translation splitStatements:true endDelimiter:; runInTransaction:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stories' AND column_name = 'translated_content'
--comment Add translation slot to stories for Spec H (bilingual mode).
--          Purpose:    Spec H — bilingual tales allow parent to request translation to child's language.
--          Risk:       low — both columns nullable, no constraints, no data migration.
--          Reversible: yes — DROP COLUMN.
--rollback ALTER TABLE stories DROP COLUMN translated_content, DROP COLUMN translated_language;

ALTER TABLE stories
    ADD COLUMN translated_content   TEXT       NULL,
    ADD COLUMN translated_language  VARCHAR(2) NULL;
