--liquibase formatted sql

-- Migration: 022-add-monobank-recurring-fields
-- Purpose: Switch Monobank from one-off invoices to tokenized recurring.
--          Persist the Monobank walletId + cardToken needed to charge again,
--          and the next renewal timestamp consumed by the scheduler.
-- Risk: low — additive only; nullable columns; no backfill needed (zero prod users).
-- Reversible: yes — see --rollback per changeset.

--changeset kazka:022-add-monobank-wallet-fields
--comment: Persist Monobank wallet id + card token for recurring charges. Nullable because
--         entitlements from other sources (APPLE, PADDLE, GIFT) do not use them.
ALTER TABLE user_entitlements
    ADD COLUMN monobank_wallet_id  VARCHAR(64)  NULL,
    ADD COLUMN monobank_card_token VARCHAR(120) NULL,
    ADD COLUMN next_renewal_at     TIMESTAMP    NULL,
    ADD COLUMN renewal_retry_count INT          NOT NULL DEFAULT 0;
--rollback ALTER TABLE user_entitlements DROP COLUMN monobank_wallet_id, DROP COLUMN monobank_card_token, DROP COLUMN next_renewal_at, DROP COLUMN renewal_retry_count;

--changeset kazka:022-index-next-renewal-at
--comment: Scheduler scans entitlements due for renewal; index makes that selective.
CREATE INDEX idx_user_entitlements_next_renewal_at ON user_entitlements(next_renewal_at);
--rollback DROP INDEX idx_user_entitlements_next_renewal_at ON user_entitlements;
