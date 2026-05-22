package com.kazka.billing.paddle;

import com.kazka.billing.BillingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "kazka.billing.paddle", name = "api-key")
public class PaddleClientImpl implements PaddleClient {

    private static final Logger log = LoggerFactory.getLogger(PaddleClientImpl.class);

    private final WebClient http;

    public PaddleClientImpl(BillingProperties props) {
        BillingProperties.Paddle paddle = props.paddle();
        String base = paddle != null && "sandbox".equalsIgnoreCase(paddle.environment())
                ? "https://sandbox-api.paddle.com"
                : "https://api.paddle.com";
        String apiKey = paddle != null ? paddle.apiKey() : "";
        this.http = WebClient.builder()
                .baseUrl(base)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    @Override
    public Mono<String> createTransaction(String paddleProductId, String userId, String userEmail) {
        Map<String, Object> body = Map.of(
                "items", List.of(Map.of("price_id", paddleProductId, "quantity", 1)),
                "customer_email", userEmail,
                "custom_data", Map.of("kazka_user_id", userId)
        );
        return http.post().uri("/transactions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) resp.get("data");
                    if (data == null || data.get("id") == null) {
                        throw new IllegalStateException("Paddle returned no transaction id: " + resp);
                    }
                    return data.get("id").toString();
                })
                .doOnError(e -> log.warn("Paddle createTransaction failed: {}", e.getMessage()));
    }
}
