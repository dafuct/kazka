--liquibase formatted sql

--changeset kazka:009-create-processed-webhook-events
--comment: Idempotency ledger for inbound payment webhooks. Each provider event id is
--         recorded once on first processing; duplicate deliveries are rejected via the
--         (provider, event_id) unique key. Risk: low — additive, new table, no FKs.
CREATE TABLE processed_webhook_events (
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    provider     VARCHAR(16)  NOT NULL,
    event_id     VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pwe_provider_event UNIQUE (provider, event_id)
);
CREATE INDEX idx_pwe_processed_at ON processed_webhook_events (processed_at);
--rollback DROP TABLE processed_webhook_events;
