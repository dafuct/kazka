package com.kazka.auth.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.auth.AuthProperties;
import com.kazka.auth.oidc.JwksKeyStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

@Component
public class GoogleIdTokenVerifier {

    private static final Set<String> ALLOWED_ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    private final AuthProperties.Google google;
    private final JwksKeyStore keyStore;

    public GoogleIdTokenVerifier(AuthProperties.Google google) {
        this.google = google;
        this.keyStore = new JwksKeyStore(google.jwksUri(), GoogleIdTokenVerifier::parseRsaKey);
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

            PublicKey key = keyStore.findKey(kid)
                    .orElseThrow(() -> new InvalidGoogleTokenException("No matching key for kid=" + kid));

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
        } catch (InvalidGoogleTokenException invalidGoogleTokenException) {
            throw invalidGoogleTokenException;
        } catch (Exception exception) {
            throw new InvalidGoogleTokenException(exception.getMessage());
        }
    }

    private static PublicKey parseRsaKey(JsonNode jwk) throws Exception {
        if (!"RSA".equals(jwk.get("kty").asText())) {
            return null;
        }
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("n").asText()));
        BigInteger publicExponent = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("e").asText()));
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
    }

    public record Verified(String subject, String email, boolean emailVerified, String name) {}

    public static final class InvalidGoogleTokenException extends RuntimeException {
        public InvalidGoogleTokenException(String message) { super(message); }
    }
}
