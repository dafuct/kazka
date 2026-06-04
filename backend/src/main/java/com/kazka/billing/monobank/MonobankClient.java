package com.kazka.billing.monobank;

import reactor.core.publisher.Mono;

public interface MonobankClient {
    /** Create a checkout invoice; saveCard=true so we get walletId+cardToken back via webhook. */
    Mono<String> createInvoiceUrl(String planId, String userId, long priceMicro, String currency);

    /** Pay-by-token (Оплата по токену). Idempotency key prevents double-charges across retries. */
    Mono<MonobankChargeResult> chargeToken(String walletId, String cardToken,
                                           long priceMicro, String currency,
                                           String idempotencyKey, String reference);

    /** Delete a tokenized card. Called on account deletion, NOT on cancel. */
    Mono<Void> deleteCard(String walletId, String cardToken);
}
