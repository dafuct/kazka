--liquibase formatted sql

-- Migration: 021-remove-liqpay-from-subscription-products
-- Purpose: LiqPay is being removed from the codebase. Drop its plan id column.
-- Risk: low — column is unused in production; no rows depend on it.
-- Reversible: yes — column can be re-added; data is throwaway placeholder values.

--changeset kazka:021-drop-liqpay-plan-id
--comment: Remove the LiqPay product mapping column from subscription_products.
ALTER TABLE subscription_products DROP COLUMN liqpay_plan_id;
--rollback ALTER TABLE subscription_products ADD COLUMN liqpay_plan_id VARCHAR(120) NULL;
