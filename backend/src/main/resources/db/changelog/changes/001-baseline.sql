--liquibase formatted sql

--changeset kazka:001-baseline splitStatements:true endDelimiter:; runInTransaction:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users'
--comment Baseline schema from pre-Liquibase schema.sql. Defines users, email_verification_tokens, password_reset_tokens, stories, flagged_attempts. Precondition makes this a no-op on existing dev/prod databases that already have the users table (the changeset is recorded as MARK_RAN in DATABASECHANGELOG).
--rollback empty

CREATE TABLE users (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(72)  NULL,
    google_subject  VARCHAR(255) NULL,
    display_name    VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    suspended_at      DATETIME(3) NULL,
    suspended_reason  VARCHAR(40) NULL,
    suspended_by      VARCHAR(36) NULL,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_google_subject (google_subject)
);

CREATE TABLE email_verification_tokens (
    token        VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id      VARCHAR(36)  NOT NULL,
    expires_at   DATETIME(3)  NOT NULL,
    consumed_at  DATETIME(3)  NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_evt_user (user_id)
);

CREATE TABLE password_reset_tokens (
    token        VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id      VARCHAR(36)  NOT NULL,
    expires_at   DATETIME(3)  NOT NULL,
    consumed_at  DATETIME(3)  NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_prt_user (user_id)
);

CREATE TABLE stories (
    id                       VARCHAR(36)   NOT NULL PRIMARY KEY,
    user_id                  VARCHAR(36)   NOT NULL,
    title                    TEXT          NOT NULL,
    theme                    TEXT          NOT NULL,
    characters               JSON          NOT NULL,
    age_group                VARCHAR(10)   NOT NULL,
    length                   VARCHAR(10)   NOT NULL,
    language                 VARCHAR(5)    NOT NULL DEFAULT 'uk',
    content                  LONGTEXT      NOT NULL,
    illustration_path_light  VARCHAR(500)  NULL,
    illustration_path_dark   VARCHAR(500)  NULL,
    illustration_status      VARCHAR(20)   NOT NULL DEFAULT 'pending',
    created_at               DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at               DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_stories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_stories_user_created (user_id, created_at DESC)
);

CREATE TABLE flagged_attempts (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id         VARCHAR(36)  NOT NULL,
    pipeline        VARCHAR(20)  NOT NULL,
    category        VARCHAR(40)  NOT NULL,
    language        VARCHAR(5)   NOT NULL,
    prompt_text     TEXT         NOT NULL,
    confidence      DECIMAL(4,3) NULL,
    judge_model     VARCHAR(100) NULL,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_fa_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_fa_user_created (user_id, created_at DESC),
    INDEX idx_fa_created (created_at)
);
