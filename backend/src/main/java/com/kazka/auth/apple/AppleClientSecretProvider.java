package com.kazka.auth.apple;

import com.kazka.auth.AuthProperties;
import io.jsonwebtoken.Jwts;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.commons.lang3.StringUtils.*;

@Component
public class AppleClientSecretProvider {

    private final AuthProperties.Apple apple;
    private final PrivateKey privateKey;
    private final AtomicReference<Cached> cache = new AtomicReference<>();

    public AppleClientSecretProvider(AuthProperties.Apple apple) {
        this.apple = apple;
        this.privateKey = parsePem(apple.privateKeyPem());
    }

    public String get() {
        Cached current = cache.get();
        if (current != null && current.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return current.jwt();
        }
        Instant now = Instant.now();
        Instant exp = now.plus(apple.clientSecretTtl());
        String jwt = Jwts.builder()
                .header().keyId(apple.keyId()).and()
                .issuer(apple.teamId())
                .subject(apple.clientId())
                .audience().add(apple.issuer()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();
        cache.set(new Cached(jwt, exp));
        return jwt;
    }

    private static PrivateKey parsePem(String pem) {
        if (isEmpty(pem) || isBlank(pem)) {
            return null;
        }
        String body = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(body);
        try {
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse Apple .p8 key", exception);
        }
    }

    private record Cached(String jwt, Instant expiresAt) {}
}
