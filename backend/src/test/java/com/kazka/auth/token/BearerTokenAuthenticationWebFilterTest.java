package com.kazka.auth.token;

import com.kazka.auth.AuthProperties;
import com.kazka.auth.KazkaUserDetails;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BearerTokenAuthenticationWebFilterTest {

    TokenIssuer issuer;
    UserRepository users;
    BearerTokenAuthenticationWebFilter filter;
    User user;

    @BeforeEach
    void setUp() {
        var jwt = new AuthProperties.Jwt(
                "test-secret-32-characters-minimum-for-hs256-signing-ok",
                Duration.ofMinutes(5),
                Duration.ofHours(1),
                "kazka-test");
        issuer = new TokenIssuer(jwt);
        users = mock(UserRepository.class);
        filter = new BearerTokenAuthenticationWebFilter(issuer, users);

        user = new User();
        user.setId("user-7");
        user.setEmail("alice@example.com");
        user.setDisplayName("Alice");
        user.setRole(UserRole.USER);
        when(users.findById("user-7")).thenReturn(Optional.of(user));
    }

    @Test
    void should_populateSecurityContext_when_validBearerHeaderPresent() {
        String token = issuer.issueAccessToken("user-7", UserRole.USER);
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
        WebFilterChain chain = exch ->
                ReactiveSecurityContextHolder.getContext()
                        .doOnNext(ctx -> assertThat(
                                ((KazkaUserDetails) ctx.getAuthentication().getPrincipal()).getUserId())
                                .isEqualTo("user-7"))
                        .then();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    @Test
    void should_passThroughUnauthenticated_when_noBearerHeader() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/me"));
        WebFilterChain chain = exch ->
                ReactiveSecurityContextHolder.getContext()
                        .doOnNext(ctx -> { throw new AssertionError("should be empty"); })
                        .then();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    @Test
    void should_passThroughUnauthenticated_when_bearerHeaderInvalid() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"));
        WebFilterChain chain = exch ->
                ReactiveSecurityContextHolder.getContext()
                        .doOnNext(ctx -> { throw new AssertionError("should be empty"); })
                        .then();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }
}
