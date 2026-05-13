--liquibase formatted sql

--changeset kazka:005-create-subscription-products
--comment: Catalogue of Pro tier products mirrored from App Store Connect.
CREATE TABLE subscription_products (
    id               VARCHAR(36)  NOT NULL PRIMARY KEY,
    apple_product_id VARCHAR(120) NOT NULL,
    name             VARCHAR(120) NOT NULL,
    price_micro      BIGINT       NOT NULL,
    currency         VARCHAR(8)   NOT NULL,
    period           VARCHAR(16)  NOT NULL,
    tier             VARCHAR(32)  NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_subscription_products_apple_id UNIQUE (apple_product_id)
);
--rollback DROP TABLE subscription_products;

--changeset kazka:005-create-user-entitlements
--comment: Per-user entitlements driven by StoreKit verify + ASN V2 webhook. Source of truth.
CREATE TABLE user_entitlements (
    id                      VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id                 VARCHAR(36)  NOT NULL,
    product_id              VARCHAR(36)  NOT NULL,
    state                   VARCHAR(24)  NOT NULL,
    expires_at              TIMESTAMP    NULL,
    latest_jws              LONGTEXT     NULL,
    original_transaction_id VARCHAR(64)  NULL,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_entitlements_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_entitlements_product FOREIGN KEY (product_id) REFERENCES subscription_products(id)
);
CREATE INDEX idx_user_entitlements_user_id ON user_entitlements(user_id);
CREATE INDEX idx_user_entitlements_product_id ON user_entitlements(product_id);
CREATE INDEX idx_user_entitlements_original_txn ON user_entitlements(original_transaction_id);
--rollback DROP TABLE user_entitlements;

--changeset kazka:005-seed-products
--comment: Seed the two Pro products. Apple product IDs match App Store Connect.
INSERT INTO subscription_products (id, apple_product_id, name, price_micro, currency, period, tier)
VALUES
  (UUID(), 'kazka_pro_monthly', 'Kazka Pro (monthly)', 4990000, 'USD', 'P1M', 'pro'),
  (UUID(), 'kazka_pro_yearly',  'Kazka Pro (yearly)', 39990000, 'USD', 'P1Y', 'pro');
--rollback DELETE FROM subscription_products WHERE apple_product_id IN ('kazka_pro_monthly', 'kazka_pro_yearly');
