package com.kazka.auth.google;

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
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class GoogleIdTokenVerifier {

    private static final Duration JWKS_CACHE_TTL = Duration.ofMinutes(15);
    private static final Set<String> ALLOWED_ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    private final AuthProperties.Google google;
    private final WebClient webClient = WebClient.create();
    private final AtomicReference<JwksCache> jwksCache = new AtomicReference<>();

    public GoogleIdTokenVerifier(AuthProperties.Google google) {
        this.google = google;
    }

    public Verified verify(String idToken) {
        try {
            int firstDot = idToken.indexOf('.');
            if (firstDot < 1) {
                throw new InvalidGoogleTokenException("Token format invalid");
            }
            String headerJson = new String(Base64.getUrlDecoder()
                    .decode(idToken.substring(0, firstDot)));
            JsonNode header = new ObjectMapper().readTree(headerJson);
            JsonNode kidNode = header.get("kid");
            if (kidNode == null) {
                throw new InvalidGoogleTokenException("Token has no kid header");
            }
            String kid = kidNode.asText();

            PublicKey key = jwksByKid().get(kid);
            if (key == null) {
                throw new InvalidGoogleTokenException("No matching key for kid=" + kid);
            }

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();

            if (!ALLOWED_ISSUERS.contains(claims.getIssuer())) {
                throw new InvalidGoogleTokenException("Issuer not allowed: " + claims.getIssuer());
            }
            Set<String> auds = claims.getAudience() == null
                    ? Set.of()
                    : claims.getAudience();
            if (google.iosClientId() == null || google.iosClientId().isBlank()
                    || !auds.contains(google.iosClientId())) {
                throw new InvalidGoogleTokenException("Audience not allowed: " + auds);
            }
            if (claims.getExpiration() == null
                    || claims.getExpiration().toInstant().isBefore(Instant.now())) {
                throw new InvalidGoogleTokenException("Token expired");
            }

            Boolean emailVerifiedClaim = claims.get("email_verified", Boolean.class);
            return new Verified(
                    claims.getSubject(),
                    claims.get("email", String.class),
                    emailVerifiedClaim != null && emailVerifiedClaim,
                    claims.get("name", String.class));
        } catch (InvalidGoogleTokenException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidGoogleTokenException(e.getMessage());
        } catch (Exception e) {
            throw new InvalidGoogleTokenException(e.getMessage());
        }
    }

    private Map<String, PublicKey> jwksByKid() {
        JwksCache current = jwksCache.get();
        if (current != null && current.fetchedAt().plus(JWKS_CACHE_TTL).isAfter(Instant.now())) {
            return current.keys();
        }
        String body = webClient.get().uri(URI.create(google.jwksUri()))
                .retrieve().bodyToMono(String.class)
                .block(Duration.ofSeconds(5));
        Map<String, PublicKey> parsed = new HashMap<>();
        try {
            JsonNode root = new ObjectMapper().readTree(body);
            for (JsonNode key : root.get("keys")) {
                if (!"RSA".equals(key.get("kty").asText())) continue;
                String kid = key.get("kid").asText();
                BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("n").asText()));
                BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("e").asText()));
                PublicKey pub = KeyFactory.getInstance("RSA")
                        .generatePublic(new RSAPublicKeySpec(n, e));
                parsed.put(kid, pub);
            }
        } catch (Exception ex) {
            throw new InvalidGoogleTokenException("Failed to parse Google JWKs: " + ex.getMessage());
        }
        jwksCache.set(new JwksCache(parsed, Instant.now()));
        return parsed;
    }

    public record Verified(String subject, String email, boolean emailVerified, String name) {}

    private record JwksCache(Map<String, PublicKey> keys, Instant fetchedAt) {}

    public static final class InvalidGoogleTokenException extends RuntimeException {
        public InvalidGoogleTokenException(String message) { super(message); }
    }
}
