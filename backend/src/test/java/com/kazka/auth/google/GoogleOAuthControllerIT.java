package com.kazka.auth.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
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
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

class GoogleOAuthControllerIT extends AbstractIT {

    static WireMockServer wiremock;
    static RSAPrivateKey googleKey;
    static String kid = "test-google-kid-it";
    static String iosClientId = "ios.client.it.googleusercontent.com";

    @Autowired UserRepository users;
    @Autowired com.kazka.billing.UserEntitlementRepository entitlementRepo;

    @BeforeAll
    static void startWiremock() throws Exception {
        wiremock = new WireMockServer(wireMockConfig().dynamicPort());
        wiremock.start();

        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        googleKey = (RSAPrivateKey) kp.getPrivate();
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();

        var jwks = Map.of("keys", new Object[]{
                Map.of("kty", "RSA", "kid", kid, "alg", "RS256", "use", "sig",
                        "n", base64UrlBigInt(pub.getModulus().toByteArray()),
                        "e", base64UrlBigInt(pub.getPublicExponent().toByteArray()))
        });
        wiremock.stubFor(get(urlEqualTo("/oauth2/v3/certs"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody(new ObjectMapper().writeValueAsString(jwks))));
    }

    @AfterAll
    static void stopWiremock() { wiremock.stop(); }

    @DynamicPropertySource
    static void googleProps(DynamicPropertyRegistry registry) {
        registry.add("kazka.auth.google.jwks-uri", () -> wiremock.baseUrl() + "/oauth2/v3/certs");
        registry.add("kazka.auth.google.ios-client-id", () -> iosClientId);
        registry.add("kazka.auth.google.issuer", () -> "https://accounts.google.com");
    }

    @BeforeEach
    void clean() {
        entitlementRepo.deleteAll();
        users.deleteAll();
    }

    @Test
    void should_returnTokensAndCreateUser_when_validGoogleIdToken() {
        String idToken = signGoogleToken("g-sub-new", "new@example.com", "New User");

        client().post().uri("/api/auth/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("idToken", idToken))
                .exchange()
                .expectStatus().isOk()
                .expectCookie().exists("SESSION")
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.refreshToken").isNotEmpty()
                .jsonPath("$.user.email").isEqualTo("new@example.com");
    }

    @Test
    void should_linkByEmail_when_subjectUnknownButEmailExists() {
        User existing = new User();
        existing.setId(UUID.randomUUID().toString());
        existing.setEmail("known@example.com");
        existing.setDisplayName("Existing");
        existing.setRole(UserRole.USER);
        existing.setEmailVerified(false);
        users.save(existing);

        String idToken = signGoogleToken("g-sub-link", "known@example.com", "Existing");

        client().post().uri("/api/auth/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("idToken", idToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.user.id").isEqualTo(existing.getId())
                .jsonPath("$.user.emailVerified").isEqualTo(true);
    }

    @Test
    void should_return401_when_identityTokenForged() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        RSAPrivateKey foreign = (RSAPrivateKey) gen.generateKeyPair().getPrivate();
        String token = Jwts.builder()
                .header().keyId(kid).and()
                .issuer("https://accounts.google.com")
                .audience().add(iosClientId).and()
                .subject("attacker")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(foreign, Jwts.SIG.RS256)
                .compact();

        client().post().uri("/api/auth/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("idToken", token))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.error").isEqualTo("INVALID_GOOGLE_TOKEN");
    }

    private String signGoogleToken(String sub, String email, String name) {
        return Jwts.builder()
                .header().keyId(kid).and()
                .issuer("https://accounts.google.com")
                .audience().add(iosClientId).and()
                .subject(sub)
                .claim("email", email)
                .claim("email_verified", true)
                .claim("name", name)
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(googleKey, Jwts.SIG.RS256)
                .compact();
    }

    private static String base64UrlBigInt(byte[] bytes) {
        int start = (bytes.length > 1 && bytes[0] == 0) ? 1 : 0;
        byte[] trimmed = new byte[bytes.length - start];
        System.arraycopy(bytes, start, trimmed, 0, trimmed.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(trimmed);
    }
}
