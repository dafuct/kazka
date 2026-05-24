--liquibase formatted sql

--changeset kazka:010-create-child-profiles splitStatements:true endDelimiter:; runInTransaction:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'child_profiles'
--comment Create child_profiles table. Each account can own multiple child profiles; tales become attributable to a specific child.
--          Purpose:   Spec C foundation — every downstream feature keys off child_profile.id.
--          Risk:      low — additive table, no existing data touched.
--          Reversible: yes — DROP TABLE child_profiles.
--rollback DROP TABLE child_profiles;

CREATE TABLE child_profiles (
    id                  VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id             VARCHAR(36)  NOT NULL,
    name                VARCHAR(80)  NOT NULL,
    birth_year          SMALLINT     NULL,
    gender              VARCHAR(10)  NULL,
    preferred_language  VARCHAR(10)  NOT NULL DEFAULT 'uk',
    interests           JSON         NOT NULL,
    avatar_seed         VARCHAR(40)  NOT NULL,
    archived_at         DATETIME(3)  NULL,
    created_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_child_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_child_profiles_user_active  (user_id, archived_at),
    INDEX idx_child_profiles_user_created (user_id, created_at DESC)
);
