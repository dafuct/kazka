--liquibase formatted sql

-- Migration: V008__billing_multi_provider
-- Purpose: Add source column to user_entitlements + web provider IDs to subscription_products so non-Apple providers can write entitlements.
-- Risk: low — additive only; defaults preserve existing Apple-IAP behavior.
-- Reversible: yes — see --rollback directives per changeset.

--changeset kazka:008-add-entitlement-source
--comment: Add source provider to user_entitlements so Apple/Paddle/LiqPay/Monobank entitlements share one table. Default 'apple' kept at DB level so existing IAP inserts continue to work until the entity is updated in Task 2.
ALTER TABLE user_entitlements
    ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'apple';
CREATE INDEX idx_user_entitlements_source ON user_entitlements(source);
--rollback DROP INDEX idx_user_entitlements_source ON user_entitlements; ALTER TABLE user_entitlements DROP COLUMN source;

--changeset kazka:008-add-product-provider-ids
--comment: Web subscription products need provider-specific IDs alongside the Apple product ID.
ALTER TABLE subscription_products
    ADD COLUMN paddle_product_id  VARCHAR(120) NULL,
    ADD COLUMN liqpay_plan_id     VARCHAR(120) NULL,
    ADD COLUMN monobank_plan_id   VARCHAR(120) NULL;
--rollback ALTER TABLE subscription_products DROP COLUMN paddle_product_id, DROP COLUMN liqpay_plan_id, DROP COLUMN monobank_plan_id;

--changeset kazka:008-update-product-rows
--comment: Backfill web provider IDs for the existing Pro monthly/yearly rows. Real values must be set per environment via overrides.
UPDATE subscription_products
SET paddle_product_id = CONCAT('pro_', period, '_placeholder')
WHERE apple_product_id IN ('kazka_pro_monthly', 'kazka_pro_yearly');
--rollback UPDATE subscription_products SET paddle_product_id = NULL WHERE apple_product_id IN ('kazka_pro_monthly', 'kazka_pro_yearly');
