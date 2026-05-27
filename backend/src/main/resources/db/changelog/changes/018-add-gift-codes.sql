--liquibase formatted sql

--changeset kazka:018-add-gift-codes splitStatements:true endDelimiter:; runInTransaction:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'gift_codes'
--comment Add gift_codes table for Spec I redemption flow.
--          Purpose:    Spec I — gift codes allow parent to create codes redeemable by others for extended subscription time.
--          Risk:       low — new table, no foreign keys, self-contained.
--          Reversible: yes — DROP TABLE.
--rollback DROP TABLE gift_codes;

CREATE TABLE gift_codes (
    code            VARCHAR(20)  NOT NULL PRIMARY KEY,
    duration_days   INT          NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'AVAILABLE',
    redeemed_by     VARCHAR(36)  NULL,
    redeemed_at     DATETIME(6)  NULL,
    expires_at      DATETIME(6)  NULL,
    note            VARCHAR(255) NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_gift_codes_redeemed_by (redeemed_by),
    INDEX idx_gift_codes_status (status)
);
