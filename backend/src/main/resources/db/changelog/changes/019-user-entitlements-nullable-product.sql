--liquibase formatted sql

-- Migration: 019__user_entitlements_nullable_product
-- Purpose: Allow gift entitlements that have no matching subscription_products row.
--          Gift codes grant time-based Pro access without a catalog product; the FK
--          on product_id must be dropped and the column made nullable so GIFT-source
--          entitlements can be persisted without a sentinel row in subscription_products.
-- Risk: low — additive relaxation; existing APPLE/PADDLE/LIQPAY rows keep their non-null values.
-- Reversible: yes — re-add FK (would need all nulls filled first)

--changeset kazka:019-drop-entitlements-product-fk splitStatements:true endDelimiter:;
--comment: Drop FK so product_id can be null for GIFT entitlements.
ALTER TABLE user_entitlements
    DROP FOREIGN KEY fk_user_entitlements_product;
--rollback ALTER TABLE user_entitlements ADD CONSTRAINT fk_user_entitlements_product FOREIGN KEY (product_id) REFERENCES subscription_products(id);

--changeset kazka:019-nullable-product-id splitStatements:true endDelimiter:;
--comment: Allow NULL product_id for non-catalog entitlement sources (GIFT).
ALTER TABLE user_entitlements
    MODIFY COLUMN product_id VARCHAR(36) NULL;
--rollback ALTER TABLE user_entitlements MODIFY COLUMN product_id VARCHAR(36) NOT NULL;
