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
        return (_, _, _, _) -> Mono.error(
                new IllegalStateException("Monobank not configured"));
    }
}
