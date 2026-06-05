--liquibase formatted sql

-- Migration: 023-rename-paddle-to-paypro
-- Purpose: rename paddle_product_id column to paypro_product_id in subscription_products,
--          and update user_entitlements.source rows from 'PADDLE' to 'PAYPRO'
--          after replacing Paddle with PayPro Global as the non-UA web checkout provider.
-- Risk: medium — column rename + enum value update. Production assumed to have zero
--                rows with source='PADDLE' at apply time (verified in pre-flight step).
-- Reversible: column rename is fully reversible. Source-value update is intentionally
--             marked irreversible to avoid mislabelling live PAYPRO rows written
--             post-deploy. Manual reversal would require a created_at cutoff scoped
--             to before this migration's apply time.

--changeset kazka:023-rename-paypro-column
ALTER TABLE subscription_products
    CHANGE COLUMN paddle_product_id paypro_product_id VARCHAR(120) NULL;
--rollback ALTER TABLE subscription_products CHANGE COLUMN paypro_product_id paddle_product_id VARCHAR(120) NULL;

--changeset kazka:023-rename-paypro-source
UPDATE user_entitlements SET source = 'PAYPRO' WHERE source = 'PADDLE';
-- Once PAYPRO is in use post-deploy, "WHERE source='PAYPRO'" would corrupt live rows.
-- Manual reversal only — scope to a pre-deploy created_at cutoff. See runbook.
--rollback empty
