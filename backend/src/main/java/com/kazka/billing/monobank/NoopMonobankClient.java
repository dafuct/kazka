package com.kazka.billing.monobank;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class NoopMonobankClient {
    @Bean
    @ConditionalOnMissingBean(MonobankClient.class)
    MonobankClient noopMonobankClient() {
        return new MonobankClient() {
            @Override
            public Mono<String> createInvoiceUrl(String planId, String userId, long priceMicro, String currency) {
                return Mono.error(new IllegalStateException("Monobank not configured"));
            }

            @Override
            public Mono<MonobankChargeResult> chargeToken(String walletId, String cardToken,
                                                          long priceMicro, String currency,
                                                          String idempotencyKey, String reference) {
                return Mono.error(new IllegalStateException("Monobank not configured"));
            }

            @Override
            public Mono<Void> deleteCard(String walletId, String cardToken) {
                return Mono.error(new IllegalStateException("Monobank not configured"));
            }
        };
    }
}
