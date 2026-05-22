package com.kazka.billing.monobank;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class StubMonobankClient {

    @Bean
    @ConditionalOnMissingBean(MonobankClient.class)
    MonobankClient monobankClient() {
        return (planId, userId, priceMicro, currency) -> Mono.error(
                new IllegalStateException("MonobankClient not yet implemented."));
    }
}
