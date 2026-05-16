package com.kazka.auth.apple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.kazka.auth.AuthProperties;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleIdentityTokenVerifierTest {

    WireMockServer wiremock;
    AppleIdentityTokenVerifier verifier;
    ECPrivateKey privateKey;
    ECPublicKey publicKey;
    String kid = "test-kid-1";

    @BeforeEach
    void setUp() throws Exception {
        wiremock = new WireMockServer(wireMockConfig().dynamicPort());
        wiremock.start();

        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = gen.generateKeyPair();
        privateKey = (ECPrivateKey) kp.getPrivate();
        publicKey = (ECPublicKey) kp.getPublic();

        var jwks = Map.of("keys", new Object[]{
                Map.of(
                        "kty", "EC",
                        "kid", kid,
                        "alg", "ES256",
                        "crv", "P-256",
                        "x", base64UrlOf(publicKey.getW().getAffineX().toByteArray()),
                        "y", base64UrlOf(publicKey.getW().getAffineY().toByteArray()))
        });
        wiremock.stubFor(get(urlEqualTo("/auth/keys"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody(new ObjectMapper().writeValueAsString(jwks))));

        var apple = new AuthProperties.Apple(
                "TEAM", "app.kazka.ios", null, "k", "",
                wiremock.baseUrl() + "/auth/keys",
                "https://appleid.apple.com",
                Duration.ofMinutes(60));
        verifier = new AppleIdentityTokenVerifier(apple);
    }

    @AfterEach
    void tearDown() {
        wiremock.stop();
    }

    @Test
    void should_returnVerifiedClaims_when_validTokenProvided() {
        String token = signToken("apple-user-123", "user@privaterelay.appleid.com", Instant.now().plus(Duration.ofMinutes(5)));

        AppleIdentityTokenVerifier.Verified verified = verifier.verify(token);

        assertThat(verified.subject()).isEqualTo("apple-user-123");
        assertThat(verified.email()).isEqualTo("user@privaterelay.appleid.com");
    }

    @Test
    void should_throw_when_audMismatched() {
        String token = Jwts.builder()
                .header().keyId(kid).and()
                .issuer("https://appleid.apple.com")
                .audience().add("some-other-app").and()
                .subject("u")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(AppleIdentityTokenVerifier.InvalidAppleTokenException.class);
    }

    @Test
    void should_acceptToken_when_audienceIsWebClientId() {
        var appleWithWeb = new AuthProperties.Apple(
                "TEAM", "app.kazka.ios", "app.kazka.web", "k", "",
                wiremock.baseUrl() + "/auth/keys",
                "https://appleid.apple.com",
                Duration.ofMinutes(60));
        var verifier2 = new AppleIdentityTokenVerifier(appleWithWeb);

        String token = Jwts.builder()
                .header().keyId(kid).and()
                .issuer("https://appleid.apple.com")
                .audience().add("app.kazka.web").and()
                .subject("u")
                .claim("email", "u@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();

        AppleIdentityTokenVerifier.Verified verified = verifier2.verify(token);
        assertThat(verified.subject()).isEqualTo("u");
    }

    @Test
    void should_acceptToken_when_audienceIsIosClientIdAndWebClientIdConfigured() {
        var appleWithWeb = new AuthProperties.Apple(
                "TEAM", "app.kazka.ios", "app.kazka.web", "k", "",
                wiremock.baseUrl() + "/auth/keys",
                "https://appleid.apple.com",
                Duration.ofMinutes(60));
        var verifier2 = new AppleIdentityTokenVerifier(appleWithWeb);

        String token = Jwts.builder()
                .header().keyId(kid).and()
                .issuer("https://appleid.apple.com")
                .audience().add("app.kazka.ios").and()
                .subject("u")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();

        AppleIdentityTokenVerifier.Verified verified = verifier2.verify(token);
        assertThat(verified.subject()).isEqualTo("u");
    }

    @Test
    void should_throw_when_audIsNeither() {
        var appleWithWeb = new AuthProperties.Apple(
                "TEAM", "app.kazka.ios", "app.kazka.web", "k", "",
                wiremock.baseUrl() + "/auth/keys",
                "https://appleid.apple.com",
                Duration.ofMinutes(60));
        var verifier2 = new AppleIdentityTokenVerifier(appleWithWeb);

        String token = Jwts.builder()
                .header().keyId(kid).and()
                .issuer("https://appleid.apple.com")
                .audience().add("some-other-app").and()
                .subject("u")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();

        assertThatThrownBy(() -> verifier2.verify(token))
                .isInstanceOf(AppleIdentityTokenVerifier.InvalidAppleTokenException.class)
                .hasMessageContaining("Audience not allowed");
    }

    @Test
    void should_throw_when_tokenExpired() {
        String token = signToken("u", "e@e.com", Instant.now().minusSeconds(60));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(AppleIdentityTokenVerifier.InvalidAppleTokenException.class);
    }

    private String signToken(String sub, String email, Instant exp) {
        return Jwts.builder()
                .header().keyId(kid).and()
                .issuer("https://appleid.apple.com")
                .audience().add("app.kazka.ios").and()
                .subject(sub)
                .claim("email", email)
                .expiration(Date.from(exp))
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();
    }

    private static String base64UrlOf(byte[] bytes) {
        // strip leading zero from a sign-extended big-endian representation
        int start = (bytes.length > 32 && bytes[0] == 0) ? 1 : 0;
        byte[] trimmed = new byte[Math.min(32, bytes.length - start)];
        System.arraycopy(bytes, bytes.length - trimmed.length, trimmed, 0, trimmed.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(trimmed);
    }
}
