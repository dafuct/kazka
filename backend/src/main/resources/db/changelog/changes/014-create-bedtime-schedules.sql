--liquibase formatted sql

--changeset kazka:014-create-bedtime-schedules splitStatements:true endDelimiter:; runInTransaction:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'bedtime_schedules'
--comment Per-child bedtime schedule (1:1 with child_profiles). Sweep cron fires due rows every 5 minutes; @Async worker generates + emails.
--          Purpose:    Spec D — nightly bedtime ritual.
--          Risk:       low — additive table, ON DELETE CASCADE on the FK.
--          Reversible: yes — DROP TABLE bedtime_schedules.
--rollback DROP TABLE bedtime_schedules;

CREATE TABLE bedtime_schedules (
    child_profile_id  VARCHAR(36)  NOT NULL PRIMARY KEY,
    enabled           BOOLEAN      NOT NULL DEFAULT FALSE,
    local_time        VARCHAR(5)   NOT NULL DEFAULT '20:30',
    timezone          VARCHAR(50)  NOT NULL DEFAULT 'Europe/Kyiv',
    themes            JSON         NOT NULL,
    next_run_at       DATETIME(3)  NULL,
    last_sent_at      DATETIME(3)  NULL,
    retry_count       INT          NOT NULL DEFAULT 0,
    failed_at         DATETIME(3)  NULL,
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_bedtime_schedules_profile FOREIGN KEY (child_profile_id) REFERENCES child_profiles(id) ON DELETE CASCADE,
    INDEX idx_bedtime_schedules_due (enabled, next_run_at)
);
