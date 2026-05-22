package com.kazka.billing.liqpay;

import reactor.core.publisher.Mono;

public interface LiqPayClient {
    Mono<String> createCheckoutUrl(String liqpayPlanId, String userId, long priceMicro, String currency);
}
