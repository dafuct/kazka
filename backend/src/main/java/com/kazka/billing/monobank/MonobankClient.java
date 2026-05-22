package com.kazka.billing.monobank;

import reactor.core.publisher.Mono;

public interface MonobankClient {
    Mono<String> createInvoiceUrl(String planId, String userId, long priceMicro, String currency);
}
