package com.kazka.auth.apple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.auth.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigInteger;
import java.net.URI;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AppleIdentityTokenVerifier {

    private final AuthProperties.Apple apple;
    private final WebClient webClient = WebClient.create();
    private final AtomicReference<JwksCache> jwksCache = new AtomicReference<>();
    private static final Duration JWKS_CACHE_TTL = Duration.ofMinutes(15);

    public AppleIdentityTokenVerifier(AuthProperties.Apple apple) {
        this.apple = apple;
    }

    public Verified verify(String identityToken) {
        try {
            // Peek header to get kid
            int firstDot = identityToken.indexOf('.');
            if (firstDot < 1) {
                throw new InvalidAppleTokenException("Token format invalid");
            }
            String headerJson = new String(Base64.getUrlDecoder()
                    .decode(identityToken.substring(0, firstDot)));
            JsonNode header = new ObjectMapper().readTree(headerJson);
            JsonNode kidNode = header.get("kid");
            if (kidNode == null) {
                throw new InvalidAppleTokenException("Token has no kid header");
            }
            String kid = kidNode.asText();

            PublicKey key = jwksByKid().get(kid);
            if (key == null) {
                throw new InvalidAppleTokenException("No matching key for kid=" + kid);
            }

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(apple.issuer())
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();

            Set<String> allowedAudiences = new LinkedHashSet<>();
            allowedAudiences.add(apple.clientId());
            if (apple.webClientId() != null && !apple.webClientId().isBlank()) {
                allowedAudiences.add(apple.webClientId());
            }
            Set<String> tokenAudiences = claims.getAudience() == null
                    ? Set.of()
                    : claims.getAudience();
            boolean audMatched = tokenAudiences.stream().anyMatch(allowedAudiences::contains);
            if (!audMatched) {
                throw new InvalidAppleTokenException("Audience not allowed: " + tokenAudiences);
            }

            if (claims.getExpiration() == null
                    || claims.getExpiration().toInstant().isBefore(Instant.now())) {
                throw new InvalidAppleTokenException("Token expired");
            }

            return new Verified(
                    claims.getSubject(),
                    claims.get("email", String.class));
        } catch (InvalidAppleTokenException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidAppleTokenException(e.getMessage());
        } catch (Exception e) {
            throw new InvalidAppleTokenException(e.getMessage());
        }
    }

    private Map<String, PublicKey> jwksByKid() {
        JwksCache current = jwksCache.get();
        if (current != null && current.fetchedAt().plus(JWKS_CACHE_TTL).isAfter(Instant.now())) {
            return current.keys();
        }
        String body = webClient.get().uri(URI.create(apple.jwksUri()))
                .retrieve().bodyToMono(String.class)
                .block(Duration.ofSeconds(5));
        Map<String, PublicKey> parsed = new HashMap<>();
        try {
            JsonNode root = new ObjectMapper().readTree(body);
            ECParameterSpec p256Spec = secp256r1Spec();
            for (JsonNode key : root.get("keys")) {
                if (!"EC".equals(key.get("kty").asText())) continue;
                String kid = key.get("kid").asText();
                BigInteger x = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("x").asText()));
                BigInteger y = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("y").asText()));
                PublicKey pub = KeyFactory.getInstance("EC")
                        .generatePublic(new ECPublicKeySpec(new ECPoint(x, y), p256Spec));
                parsed.put(kid, pub);
            }
        } catch (Exception e) {
            throw new InvalidAppleTokenException("Failed to parse Apple JWKs: " + e.getMessage());
        }
        jwksCache.set(new JwksCache(parsed, Instant.now()));
        return parsed;
    }

    private static ECParameterSpec secp256r1Spec() throws Exception {
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec("secp256r1"));
        return params.getParameterSpec(ECParameterSpec.class);
    }

    public record Verified(String subject, String email) {}

    private record JwksCache(Map<String, PublicKey> keys, Instant fetchedAt) {}

    public static final class InvalidAppleTokenException extends RuntimeException {
        public InvalidAppleTokenException(String message) { super(message); }
    }
}
