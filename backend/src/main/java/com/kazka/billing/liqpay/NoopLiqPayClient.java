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
        return (_, _, _, _) -> Mono.error(
                new IllegalStateException("LiqPay not configured"));
    }
}
