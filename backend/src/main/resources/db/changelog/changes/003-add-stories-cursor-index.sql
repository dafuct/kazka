--liquibase formatted sql

--changeset kazka:003-add-stories-cursor-index splitStatements:true endDelimiter:; runInTransaction:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'stories' AND index_name = 'idx_stories_user_created_id'
--comment Compound index for cursor pagination: (user_id, created_at DESC, id DESC). Tiebreaks rows that share created_at by id so cursor (created_at, id) is strictly monotonic. Replaces the read pattern previously served by idx_stories_user_created — the old index can stay; MySQL will pick the better one for each query.
--rollback DROP INDEX idx_stories_user_created_id ON stories;

CREATE INDEX idx_stories_user_created_id ON stories (user_id, created_at DESC, id DESC);
