package com.kazka.auth.apple;

import com.kazka.auth.AuthProperties;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AppleClientSecretProviderTest {

    AppleClientSecretProvider provider;
    PublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = gen.generateKeyPair();
        publicKey = kp.getPublic();
        ECPrivateKey priv = (ECPrivateKey) kp.getPrivate();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(priv.getEncoded())
                + "\n-----END PRIVATE KEY-----";

        var apple = new AuthProperties.Apple(
                "TEAM123", "app.kazka.ios", "KEY123", pem,
                "https://example.com/jwks", "https://appleid.apple.com",
                Duration.ofMinutes(60));
        provider = new AppleClientSecretProvider(apple);
    }

    @Test
    void should_produceVerifiableJwt_when_getCalled() {
        String jwt = provider.get();

        var jws = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(jwt);
        var claims = jws.getPayload();
        assertThat(claims.getIssuer()).isEqualTo("TEAM123");
        assertThat(claims.getSubject()).isEqualTo("app.kazka.ios");
        assertThat(claims.getAudience()).containsExactly("https://appleid.apple.com");
        assertThat(jws.getHeader().getKeyId()).isEqualTo("KEY123");
        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("ES256");
    }

    @Test
    void should_returnCachedJwt_when_calledMultipleTimesWithinTtl() {
        String first = provider.get();
        String second = provider.get();
        assertThat(second).isEqualTo(first);
    }
}
