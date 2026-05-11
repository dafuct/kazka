--liquibase formatted sql

--changeset kazka:002-add-apple-oauth-columns splitStatements:true endDelimiter:; runInTransaction:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'apple_subject'
--comment Add Apple OAuth fields to users for Sign in with Apple support (M2 of iOS mobile module spec). Both columns nullable (Apple subject only populated for users who sign in with Apple). Unique index on apple_subject enforces one-to-one user↔Apple mapping. Precondition makes the changeset idempotent if the apple_subject column already exists.
--rollback ALTER TABLE users DROP INDEX uk_users_apple_subject;
--rollback ALTER TABLE users DROP COLUMN apple_email_relay;
--rollback ALTER TABLE users DROP COLUMN apple_subject;

ALTER TABLE users
    ADD COLUMN apple_subject VARCHAR(255) NULL AFTER google_subject,
    ADD COLUMN apple_email_relay VARCHAR(255) NULL AFTER apple_subject,
    ADD UNIQUE KEY uk_users_apple_subject (apple_subject);
