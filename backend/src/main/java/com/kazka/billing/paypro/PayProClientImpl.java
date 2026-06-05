package com.kazka.billing.paypro;

import com.kazka.billing.BillingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "kazka.billing.paypro", name = "api-key")
public class PayProClientImpl implements PayProClient {

    private static final String DEFAULT_BASE = "https://store.payproglobal.com";

    private final WebClient http;
    private final BillingProperties.PayPro paypro;

    @Autowired
    public PayProClientImpl(BillingProperties props) {
        this(props, DEFAULT_BASE);
    }

    // Test-only constructor: lets the test point at MockWebServer.
    PayProClientImpl(BillingProperties props, String baseUrl) {
        this.paypro = props.paypro();
        this.http = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Mono<Void> terminate(String subscriptionId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vendorAccountId", paypro.vendorId());
        body.put("apiSecretKey", paypro.apiKey());
        body.put("subscriptionId", subscriptionId);
        body.put("sendCustomerNotification", true);
        body.put("reasonText", "User requested cancellation");

        return http.post().uri("/api/Subscriptions/Terminate")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(r -> log.info("PayPro terminate ok sub={}", subscriptionId))
                .then()
                .onErrorMap(WebClientResponseException.class, ex -> {
                    log.warn("PayPro terminate failed sub={} status={} body={}",
                            subscriptionId, ex.getStatusCode(), ex.getResponseBodyAsString());
                    return new IllegalStateException(
                            "PayPro terminate failed sub=" + subscriptionId
                                    + " (" + ex.getStatusCode() + ")", ex);
                });
    }
}
