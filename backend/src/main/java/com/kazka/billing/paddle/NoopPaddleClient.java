package com.kazka.billing.paddle;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class NoopPaddleClient {
    @Bean
    @ConditionalOnMissingBean(PaddleClient.class)
    PaddleClient noopPaddleClient() {
        return (productId, userId, email) -> Mono.error(
                new IllegalStateException("Paddle not configured (set PADDLE_API_KEY)"));
    }
}
