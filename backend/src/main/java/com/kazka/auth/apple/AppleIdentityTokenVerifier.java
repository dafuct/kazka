package com.kazka.auth.apple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.auth.AuthProperties;
import com.kazka.auth.oidc.JwksKeyStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class AppleIdentityTokenVerifier {

    private final AuthProperties.Apple apple;
    private final JwksKeyStore keyStore;

    public AppleIdentityTokenVerifier(AuthProperties.Apple apple) {
        this.apple = apple;
        this.keyStore = new JwksKeyStore(apple.jwksUri(), AppleIdentityTokenVerifier::parseEcKey);
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

            PublicKey key = keyStore.findKey(kid)
                    .orElseThrow(() -> new InvalidAppleTokenException("No matching key for kid=" + kid));

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
        } catch (InvalidAppleTokenException invalidAppleTokenException) {
            throw invalidAppleTokenException;
        } catch (Exception exception) {
            throw new InvalidAppleTokenException(exception.getMessage());
        }
    }

    private static PublicKey parseEcKey(JsonNode jwk) throws Exception {
        if (!"EC".equals(jwk.get("kty").asText())) {
            return null;
        }
        BigInteger pointX = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("x").asText()));
        BigInteger pointY = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("y").asText()));
        return KeyFactory.getInstance("EC")
                .generatePublic(new ECPublicKeySpec(new ECPoint(pointX, pointY), secp256r1Spec()));
    }

    private static ECParameterSpec secp256r1Spec() throws Exception {
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec("secp256r1"));
        return params.getParameterSpec(ECParameterSpec.class);
    }

    public record Verified(String subject, String email) {}

    public static final class InvalidAppleTokenException extends RuntimeException {
        public InvalidAppleTokenException(String message) { super(message); }
    }
}
