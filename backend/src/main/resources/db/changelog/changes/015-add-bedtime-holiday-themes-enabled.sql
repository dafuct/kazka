--liquibase formatted sql

--changeset kazka:015-add-bedtime-holiday-themes-enabled splitStatements:true endDelimiter:; runInTransaction:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bedtime_schedules' AND column_name = 'holiday_themes_enabled'
--comment Add per-child opt-out toggle for Spec G holiday auto-themes.
--          Purpose:    Spec G — per-child opt-out for holiday-flavored bedtime tales.
--          Risk:       low — additive column, NOT NULL with DEFAULT TRUE.
--          Reversible: yes — DROP COLUMN.
--rollback ALTER TABLE bedtime_schedules DROP COLUMN holiday_themes_enabled;

ALTER TABLE bedtime_schedules
    ADD COLUMN holiday_themes_enabled BOOLEAN NOT NULL DEFAULT TRUE;
