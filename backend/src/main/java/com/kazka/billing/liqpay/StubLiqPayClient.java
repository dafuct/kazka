package com.kazka.billing.liqpay;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class StubLiqPayClient {

    @Bean
    @ConditionalOnMissingBean(LiqPayClient.class)
    LiqPayClient liqPayClient() {
        return (planId, userId, priceMicro, currency) -> Mono.error(
                new IllegalStateException("LiqPayClient not yet implemented."));
    }
}
