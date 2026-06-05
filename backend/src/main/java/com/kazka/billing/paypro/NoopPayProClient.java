package com.kazka.billing.paypro;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class NoopPayProClient {
    @Bean
    @ConditionalOnMissingBean(PayProClient.class)
    PayProClient noopPayProClient() {
        return _ -> Mono.error(
                new IllegalStateException("PayPro not configured (set PAYPRO_API_KEY)"));
    }
}
