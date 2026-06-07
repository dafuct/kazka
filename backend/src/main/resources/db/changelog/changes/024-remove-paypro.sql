--liquibase formatted sql

-- Migration: 024-remove-paypro
-- Purpose: PayPro Global is removed from the codebase. Drop its product-mapping
--          column and retire the PAYPRO entitlement source value.
-- Risk: medium — DROP COLUMN on a config table + retiring an enum string value.
--               PayPro was never surfaced in the checkout UI (only Monobank/UA was
--               offered, "coming soon" elsewhere), so production is expected to have
--               zero rows with source='PAYPRO'. The precondition below enforces that
--               assumption: if any live PAYPRO row exists the deploy HALTS, because the
--               application no longer has a PAYPRO enum constant to read it back.
-- Reversible: column re-addable (rollback below). The PAYPRO source value is not
--             re-introduced — there are no rows to relabel.

--changeset kazka:024-drop-paypro-product-id
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM user_entitlements WHERE source = 'PAYPRO'
ALTER TABLE subscription_products DROP COLUMN paypro_product_id;
--rollback ALTER TABLE subscription_products ADD COLUMN paypro_product_id VARCHAR(120) NULL;
