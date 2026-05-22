package com.kazka.billing.liqpay;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class NoopLiqPayClient {
    @Bean
    @ConditionalOnMissingBean(LiqPayClient.class)
    LiqPayClient noopLiqPayClient() {
        return (planId, userId, priceMicro, currency) -> Mono.error(
                new IllegalStateException("LiqPay not configured"));
    }
}
