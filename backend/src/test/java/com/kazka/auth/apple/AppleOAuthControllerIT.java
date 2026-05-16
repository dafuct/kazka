package com.kazka.auth.apple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.kazka.AbstractIT;
import com.kazka.user.UserRepository;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

class AppleOAuthControllerIT extends AbstractIT {

    static WireMockServer wiremock;
    static ECPrivateKey appleKey;
    static String kid = "test-kid-1";

    @Autowired UserRepository users;

    @BeforeAll
    static void startWiremock() throws Exception {
        wiremock = new WireMockServer(wireMockConfig().dynamicPort());
        wiremock.start();

        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = gen.generateKeyPair();
        appleKey = (ECPrivateKey) kp.getPrivate();
        ECPublicKey pub = (ECPublicKey) kp.getPublic();

        var jwks = Map.of("keys", new Object[]{
                Map.of("kty", "EC", "kid", kid, "alg", "ES256", "crv", "P-256",
                        "x", base64UrlOf(pub.getW().getAffineX().toByteArray()),
                        "y", base64UrlOf(pub.getW().getAffineY().toByteArray()))
        });
        wiremock.stubFor(get(urlEqualTo("/auth/keys"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody(new ObjectMapper().writeValueAsString(jwks))));
    }

    @AfterAll
    static void stopWiremock() { wiremock.stop(); }

    @DynamicPropertySource
    static void appleProps(DynamicPropertyRegistry r) {
        r.add("kazka.auth.apple.jwks-uri", () -> wiremock.baseUrl() + "/auth/keys");
        r.add("kazka.auth.apple.client-id", () -> "app.kazka.ios.test");
        r.add("kazka.auth.apple.issuer", () -> "https://appleid.apple.com");
    }

    @BeforeEach
    void clean() { users.deleteAll(); }

    @Test
    void should_returnTokensAndCreateUser_when_validAppleIdentityToken() {
        String idToken = signAppleToken("apple-sub-new", "new@privaterelay.appleid.com");

        client().post().uri("/api/auth/oauth/apple")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "identityToken", idToken,
                        "fullName", "Niu User",
                        "email", "new@privaterelay.appleid.com"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("Set-Cookie")
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.refreshToken").isNotEmpty()
                .jsonPath("$.user.email").isEqualTo("new@privaterelay.appleid.com");
    }

    @Test
    void should_return401_when_identityTokenForged() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            ECPrivateKey foreign = (ECPrivateKey) gen.generateKeyPair().getPrivate();
            String token = Jwts.builder()
                    .header().keyId(kid).and()
                    .issuer("https://appleid.apple.com")
                    .audience().add("app.kazka.ios.test").and()
                    .subject("attacker")
                    .expiration(Date.from(Instant.now().plusSeconds(60)))
                    .signWith(foreign, Jwts.SIG.ES256)
                    .compact();

            client().post().uri("/api/auth/oauth/apple")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("identityToken", token))
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody().jsonPath("$.error").isEqualTo("INVALID_APPLE_TOKEN");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private String signAppleToken(String sub, String email) {
        return Jwts.builder()
                .header().keyId(kid).and()
                .issuer("https://appleid.apple.com")
                .audience().add("app.kazka.ios.test").and()
                .subject(sub)
                .claim("email", email)
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(appleKey, Jwts.SIG.ES256)
                .compact();
    }

    private static String base64UrlOf(byte[] bytes) {
        int start = (bytes.length > 32 && bytes[0] == 0) ? 1 : 0;
        byte[] trimmed = new byte[Math.min(32, bytes.length - start)];
        System.arraycopy(bytes, bytes.length - trimmed.length, trimmed, 0, trimmed.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(trimmed);
    }
}
