package com.kazka.billing.monobank;

/**
 * Result of a pay-by-token call. Synchronous result is "accepted/not accepted";
 * the actual entitlement update lands via the renewal webhook.
 */
public sealed interface MonobankChargeResult {
    /** 2xx — Monobank accepted the charge; webhook will follow. */
    record Accepted(String invoiceId) implements MonobankChargeResult {}

    /** 4xx — non-retryable (card expired, blocked, insufficient funds). */
    record CardFailure(String reason) implements MonobankChargeResult {}

    /** 5xx / network — transient; leave row unchanged, retry next tick. */
    record Transient(String reason) implements MonobankChargeResult {}
}
