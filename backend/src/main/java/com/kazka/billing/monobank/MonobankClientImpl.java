package com.kazka.billing.monobank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.billing.BillingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "kazka.billing.monobank", name = "token")
public class MonobankClientImpl implements MonobankClient {

    private static final ObjectMapper JSON = new ObjectMapper();

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

    @Override
    public Mono<String> createInvoiceUrl(String planId, String userId, long priceMicro, String currency) {
        long amountKopecks = priceMicro / 10_000;
        int currencyCode = "UAH".equalsIgnoreCase(currency) ? 980 : 840;
        Map<String, Object> body = Map.of(
                "amount", amountKopecks,
                "ccy", currencyCode,
                "merchantPaymInfo", Map.of(
                        "reference", planId + ":" + userId,
                        "destination", "Kazka Pro subscription"),
                "redirectUrl", props.successUrl(),
                "webHookUrl", deriveWebhookUrl(),
                "validity", 3600,
                "saveCardData", Map.of("saveCard", true, "walletId", userId)
        );
        return http.post().uri("/api/merchant/invoice/create")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(MonobankClientImpl::parseJson)
                .map(json -> json.path("pageUrl").asText());
    }

    @Override
    public Mono<MonobankChargeResult> chargeToken(String walletId, String cardToken,
                                                  long priceMicro, String currency,
                                                  String idempotencyKey, String reference) {
        long amountKopecks = priceMicro / 10_000;
        int currencyCode = "UAH".equalsIgnoreCase(currency) ? 980 : 840;
        Map<String, Object> body = Map.of(
                "cardToken", cardToken,
                "amount", amountKopecks,
                "ccy", currencyCode,
                "merchantPaymInfo", Map.of(
                        "reference", reference,
                        "destination", "Kazka Pro recurring"),
                "webHookUrl", deriveWebhookUrl(),
                "initiationKind", "merchant"
        );
        return http.post().uri(uri -> uri.path("/api/merchant/wallet/payment")
                        .queryParam("walletId", walletId).build())
                .header("X-Request-Id", idempotencyKey)
                .bodyValue(body)
                .exchangeToMono(resp -> {
                    HttpStatusCode statusCode = resp.statusCode();
                    if (statusCode.is2xxSuccessful()) {
                        return resp.bodyToMono(String.class).defaultIfEmpty("{}")
                                .map(responseBody -> (MonobankChargeResult) new MonobankChargeResult.Accepted(
                                        parseJson(responseBody).path("invoiceId").asText("")));
                    }
                    if (statusCode.is4xxClientError()) {
                        return resp.bodyToMono(String.class).defaultIfEmpty("")
                                .map(responseBody -> (MonobankChargeResult) new MonobankChargeResult.CardFailure("4xx: " + statusCode.value() + " " + responseBody));
                    }
                    return resp.bodyToMono(String.class).defaultIfEmpty("")
                            .map(responseBody -> (MonobankChargeResult) new MonobankChargeResult.Transient("5xx: " + statusCode.value() + " " + responseBody));
                })
                .onErrorResume(ex -> Mono.just(new MonobankChargeResult.Transient(ex.getMessage())));
    }

    @Override
    public Mono<Void> deleteCard(String walletId, String cardToken) {
        return http.delete().uri(uri -> uri.path("/api/merchant/wallet/card")
                        .queryParam("walletId", walletId)
                        .queryParam("cardToken", cardToken).build())
                .retrieve()
                .bodyToMono(Void.class);
    }

    private String deriveWebhookUrl() {
        String successUrl = props.successUrl();
        int idx = successUrl.indexOf("/", 8);
        String origin = idx > 0 ? successUrl.substring(0, idx) : successUrl;
        return origin + "/api/billing/webhook/monobank";
    }

    private static JsonNode parseJson(String body) {
        try {
            return JSON.readTree(body == null || body.isEmpty() ? "{}" : body);
        } catch (Exception exception) {
            return JSON.createObjectNode();
        }
    }
}
