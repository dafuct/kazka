package com.kazka.billing.monobank;

import com.fasterxml.jackson.databind.JsonNode;
import com.kazka.billing.BillingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "kazka.billing.monobank", name = "token")
public class MonobankClientImpl implements MonobankClient {

    private final WebClient http;
    private final BillingProperties props;

    public MonobankClientImpl(BillingProperties props) {
        this.props = props;
        String token = props.monobank() != null ? props.monobank().token() : "";
        this.http = WebClient.builder()
                .baseUrl("https://api.monobank.ua")
                .defaultHeader("X-Token", token)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    /**
     * Creates a recurring-payment invoice via Monobank Acquiring API.
     * Returns the pageUrl the user should be redirected to.
     */
    @Override
    public Mono<String> createInvoiceUrl(String planId, String userId, long priceMicro, String currency) {
        long amountKopecks = priceMicro / 10_000;  // micros -> kopecks (1 UAH = 100 kopecks, micros / 10000)
        int currencyCode = "UAH".equalsIgnoreCase(currency) ? 980 : 840;
        Map<String, Object> body = Map.of(
                "amount", amountKopecks,
                "ccy", currencyCode,
                "merchantPaymInfo", Map.of(
                        "reference", planId + ":" + userId,
                        "destination", "Kazka Pro subscription"),
                "redirectUrl", props.successUrl(),
                "webHookUrl", deriveWebhookUrl(),
                "validity", 3600
        );
        return http.post().uri("/api/merchant/invoice/create")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(j -> j.path("pageUrl").asText());
    }

    private String deriveWebhookUrl() {
        String s = props.successUrl();
        int idx = s.indexOf("/", 8);
        String origin = idx > 0 ? s.substring(0, idx) : s;
        return origin + "/api/billing/webhook/monobank";
    }
}
