package com.kazka.auth.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.kazka.auth.AuthProperties;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
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

class GoogleIdTokenVerifierTest {

    WireMockServer wiremock;
    GoogleIdTokenVerifier verifier;
    RSAPrivateKey privateKey;
    RSAPublicKey publicKey;
    String kid = "test-google-kid-1";
    String clientId = "ios.client.googleusercontent.com";

    @BeforeEach
    void setUp() throws Exception {
        wiremock = new WireMockServer(wireMockConfig().dynamicPort());
        wiremock.start();

        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        privateKey = (RSAPrivateKey) kp.getPrivate();
        publicKey = (RSAPublicKey) kp.getPublic();

        var jwks = Map.of("keys", new Object[]{
                Map.of(
                        "kty", "RSA",
                        "kid", kid,
                        "alg", "RS256",
                        "use", "sig",
                        "n", base64UrlBigInt(publicKey.getModulus().toByteArray()),
                        "e", base64UrlBigInt(publicKey.getPublicExponent().toByteArray())
                )
        });
        wiremock.stubFor(get(urlEqualTo("/oauth2/v3/certs"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody(new ObjectMapper().writeValueAsString(jwks))));

        var google = new AuthProperties.Google(
                clientId,
                null,
                wiremock.baseUrl() + "/oauth2/v3/certs",
                "https://accounts.google.com");
        verifier = new GoogleIdTokenVerifier(google);
    }

    @AfterEach
    void tearDown() { wiremock.stop(); }

    @Test
    void should_returnVerifiedClaims_when_validTokenProvided() {
        String token = signToken("google-user-123", "user@example.com", "User Name",
                Instant.now().plusSeconds(60));

        GoogleIdTokenVerifier.Verified v = verifier.verify(token);

        assertThat(v.subject()).isEqualTo("google-user-123");
        assertThat(v.email()).isEqualTo("user@example.com");
        assertThat(v.name()).isEqualTo("User Name");
    }

    @Test
    void should_throw_when_audMismatched() {
        String token = Jwts.builder()
                .header().keyId(kid).and()
                .issuer("https://accounts.google.com")
                .audience().add("some.other.client").and()
                .subject("u")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(GoogleIdTokenVerifier.InvalidGoogleTokenException.class);
    }

    @Test
    void should_throw_when_tokenExpired() {
        String token = signToken("u", "e@e.com", "n", Instant.now().minusSeconds(60));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(GoogleIdTokenVerifier.InvalidGoogleTokenException.class);
    }

    @Test
    void should_throw_when_issuerMismatched() {
        String token = Jwts.builder()
                .header().keyId(kid).and()
                .issuer("https://evil.example.com")
                .audience().add(clientId).and()
                .subject("u")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(GoogleIdTokenVerifier.InvalidGoogleTokenException.class);
    }

    @Test
    void should_acceptIssuerWithoutScheme() {
        String token = Jwts.builder()
                .header().keyId(kid).and()
                .issuer("accounts.google.com")
                .audience().add(clientId).and()
                .subject("u")
                .claim("email", "e@e.com")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        GoogleIdTokenVerifier.Verified v = verifier.verify(token);
        assertThat(v.subject()).isEqualTo("u");
    }

    @Test
    void should_throw_when_unknownKid() {
        String token = Jwts.builder()
                .header().keyId("totally-unknown-kid").and()
                .issuer("https://accounts.google.com")
                .audience().add(clientId).and()
                .subject("u")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(GoogleIdTokenVerifier.InvalidGoogleTokenException.class);
    }

    private String signToken(String sub, String email, String name, Instant exp) {
        return Jwts.builder()
                .header().keyId(kid).and()
                .issuer("https://accounts.google.com")
                .audience().add(clientId).and()
                .subject(sub)
                .claim("email", email)
                .claim("name", name)
                .expiration(Date.from(exp))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    private static String base64UrlBigInt(byte[] bytes) {
        int start = (bytes.length > 1 && bytes[0] == 0) ? 1 : 0;
        byte[] trimmed = new byte[bytes.length - start];
        System.arraycopy(bytes, start, trimmed, 0, trimmed.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(trimmed);
    }
}
