--liquibase formatted sql

--changeset kazka:006-add-stories-this-month
--comment: Free-tier monthly story counter. Reset by MonthlyCounterResetJob on the 1st of each month.
ALTER TABLE users
    ADD COLUMN stories_this_month INT NOT NULL DEFAULT 0,
    ADD COLUMN counter_reset_at TIMESTAMP NULL DEFAULT NULL;
--rollback ALTER TABLE users DROP COLUMN stories_this_month, DROP COLUMN counter_reset_at;
