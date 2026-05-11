package com.kazka.auth.token;

import com.kazka.auth.AuthProperties;
import com.kazka.user.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class TokenIssuer {

    private final AuthProperties.Jwt jwt;
    private final SecretKey signingKey;

    public TokenIssuer(AuthProperties.Jwt jwt) {
        this.jwt = jwt;
        byte[] secretBytes = jwt.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException(
                    "kazka.auth.jwt.secret must be at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String issueAccessToken(String userId, UserRole role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(jwt.issuer())
                .subject(userId)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwt.accessTtl())))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims verifyAccessToken(String token) {
        try {
            var jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(jwt.issuer())
                    .build()
                    .parseSignedClaims(token);
            var payload = jws.getPayload();
            return new Claims(
                    payload.getSubject(),
                    UserRole.valueOf(payload.get("role", String.class)),
                    payload.getIssuer(),
                    payload.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException(e.getMessage());
        }
    }

    public record Claims(String userId, UserRole role, String issuer, Instant expiresAt) {}

    public static final class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) { super(message); }
    }
}
