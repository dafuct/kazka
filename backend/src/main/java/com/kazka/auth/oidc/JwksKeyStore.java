package com.kazka.auth.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fetches a provider's JWKS and caches the resulting public keys by {@code kid} for a fixed TTL.
 * Per-key decoding (EC, RSA, ...) is supplied by the caller via {@link JwkParser}, so the
 * fetch-and-cache plumbing is shared across providers while key-type specifics stay with them.
 */
public final class JwksKeyStore {

    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<Cache> cache = new AtomicReference<>();

    private final String jwksUri;
    private final JwkParser parser;

    public JwksKeyStore(String jwksUri, JwkParser parser) {
        this.jwksUri = jwksUri;
        this.parser = parser;
    }

    public Optional<PublicKey> findKey(String kid) {
        return Optional.ofNullable(keysByKid().get(kid));
    }

    private Map<String, PublicKey> keysByKid() {
        Cache current = cache.get();
        if (current != null && current.fetchedAt().plus(CACHE_TTL).isAfter(Instant.now())) {
            return current.keys();
        }
        String body = webClient.get().uri(URI.create(jwksUri))
                .retrieve().bodyToMono(String.class)
                .block(FETCH_TIMEOUT);
        Map<String, PublicKey> parsed = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            for (JsonNode jwk : root.get("keys")) {
                PublicKey key = parser.parse(jwk);
                if (key != null) {
                    parsed.put(jwk.get("kid").asText(), key);
                }
            }
        } catch (Exception exception) {
            throw new JwksException("Failed to parse JWKs: " + exception.getMessage());
        }
        cache.set(new Cache(parsed, Instant.now()));
        return parsed;
    }

    /** Decodes a single JWK entry into a {@link PublicKey}, or returns {@code null} to skip it. */
    @FunctionalInterface
    public interface JwkParser {
        PublicKey parse(JsonNode jwk) throws Exception;
    }

    public static final class JwksException extends RuntimeException {
        public JwksException(String message) {
            super(message);
        }
    }

    private record Cache(Map<String, PublicKey> keys, Instant fetchedAt) {}
}
