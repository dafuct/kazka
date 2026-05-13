--liquibase formatted sql

--changeset kazka:004-create-device-tokens splitStatements:true endDelimiter:; runInTransaction:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'device_tokens'
--comment Stores APNs device tokens per (user, platform). Unique on device_token alone so a token re-registering under a new user account (e.g., user signs out and a different user signs in on the same phone) replaces the prior row.
--rollback DROP TABLE device_tokens;

CREATE TABLE device_tokens (
    id            VARCHAR(36)   NOT NULL PRIMARY KEY,
    user_id       VARCHAR(36)   NOT NULL,
    device_token  VARCHAR(255)  NOT NULL,
    platform      VARCHAR(20)   NOT NULL,
    locale        VARCHAR(10)   NULL,
    created_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_device_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_device_tokens_token (device_token),
    INDEX idx_device_tokens_user (user_id)
);
