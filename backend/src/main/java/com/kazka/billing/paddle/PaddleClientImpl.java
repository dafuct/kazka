package com.kazka.billing.paddle;

import com.kazka.billing.BillingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
    public Mono<PaddleTransaction> createTransaction(String paddleProductId, String userId, String userEmail) {
        Map<String, Object> body = Map.of(
                "items", List.of(Map.of("price_id", paddleProductId, "quantity", 1)),
                "customer_email", userEmail,
                "custom_data", Map.of("kazka_user_id", userId)
        );
        return http.post().uri("/transactions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::parseTransaction)
                .onErrorMap(WebClientResponseException.class, this::translatePaddleError);
    }

    @SuppressWarnings("unchecked")
    private PaddleTransaction parseTransaction(Map<?, ?> resp) {
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        if (data == null || data.get("id") == null) {
            throw new IllegalStateException("Paddle returned no transaction id: " + resp);
        }
        String id = data.get("id").toString();
        String checkoutUrl = null;
        Object checkoutObj = data.get("checkout");
        if (checkoutObj instanceof Map<?, ?> checkout) {
            Object url = checkout.get("url");
            if (url != null) checkoutUrl = url.toString();
        }
        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            throw new IllegalStateException(
                    "Paddle returned transaction without checkout URL. " +
                    "Set a Default Payment Link in Paddle dashboard → Checkout settings. " +
                    "Response: " + resp);
        }
        return new PaddleTransaction(id, checkoutUrl);
    }

    private RuntimeException translatePaddleError(WebClientResponseException ex) {
        String responseBody = ex.getResponseBodyAsString();
        log.warn("Paddle createTransaction failed: {} — response body: {}",
                ex.getMessage(), responseBody);
        return new IllegalStateException(
                "Paddle API rejected the request (" + ex.getStatusCode() + "): " + responseBody, ex);
    }
}
