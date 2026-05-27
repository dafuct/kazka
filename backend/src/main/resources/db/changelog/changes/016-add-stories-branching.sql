--liquibase formatted sql

--changeset kazka:016-add-stories-branching splitStatements:true endDelimiter:; runInTransaction:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stories' AND column_name = 'is_branching'
--comment Add branching support to stories.
--          Purpose:    Spec F — interactive branching tales need state across multiple LLM calls.
--          Risk:       low — additive columns, NOT NULL with DEFAULT for the booleans/state.
--          Reversible: yes — DROP COLUMN.
--rollback ALTER TABLE stories DROP COLUMN is_branching, DROP COLUMN branching_state, DROP COLUMN pending_choices;

ALTER TABLE stories
    ADD COLUMN is_branching     BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN branching_state  VARCHAR(20) NOT NULL DEFAULT 'complete',
    ADD COLUMN pending_choices  JSON        NULL;
