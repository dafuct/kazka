package com.kazka.billing.monobank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.billing.BillingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class MonobankPubKeyService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final BillingProperties props;
    private final WebClient http;
    private final AtomicReference<PublicKey> cached = new AtomicReference<>();

    public MonobankPubKeyService(BillingProperties props) {
        this.props = props;
        this.http = WebClient.builder()
                .baseUrl("https://api.monobank.ua")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    public Mono<PublicKey> publicKey() {
        PublicKey cachedKey = cached.get();
        if (cachedKey != null) return Mono.just(cachedKey);
        return fetch().doOnNext(cached::set);
    }

    public void invalidate() {
        cached.set(null);
    }

    private Mono<PublicKey> fetch() {
        String token = props.monobank() != null ? props.monobank().token() : null;
        if (token == null || token.isBlank()) {
            return Mono.empty();
        }
        return http.get().uri("/api/merchant/pubkey")
                .header("X-Token", token)
                .retrieve()
                .bodyToMono(String.class)
                .map(MonobankPubKeyService::parsePubKey);
    }

    private static PublicKey parsePubKey(String responseBody) {
        try {
            JsonNode node = JSON.readTree(responseBody);
            String b64Pem = node.path("key").asText();
            String pem = new String(Base64.getDecoder().decode(b64Pem), StandardCharsets.UTF_8);
            String der = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] spki = Base64.getDecoder().decode(der);
            return KeyFactory.getInstance("EC")
                    .generatePublic(new X509EncodedKeySpec(spki));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse Monobank public key", exception);
        }
    }
}
