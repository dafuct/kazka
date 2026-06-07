package com.kazka.child;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementResolver;
import com.kazka.child.dto.ChildProfileDto;
import com.kazka.child.dto.CreateChildProfileRequest;
import com.kazka.child.dto.CreateChildProfilesBatchRequest;
import com.kazka.child.dto.UpdateChildProfileRequest;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("integration")
class ChildProfileControllerIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean EntitlementResolver entitlements;

    @BeforeEach
    void setup() {
        when(entitlements.isPro(any())).thenReturn(true);
    }

    @Test
    void should_create_then_list_profile() {
        String userA = seedUser();
        WebTestClient clientA = authedClient(userA);
        clientA.post().uri("/api/children")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateChildProfileRequest("Лія", (short) 2020, "girl", "uk", List.of("dragons")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Лія")
                .jsonPath("$.characterCount").isEqualTo(0);

        clientA.get().uri("/api/children").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void should_allow_freeTier_to_create_multiple_profiles() {
        String userA = seedUser();
        when(entitlements.isPro(userA)).thenReturn(false);
        WebTestClient clientA = authedClient(userA);
        clientA.post().uri("/api/children")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateChildProfileRequest("First", null, null, "uk", List.of()))
                .exchange().expectStatus().isOk();
        clientA.post().uri("/api/children")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateChildProfileRequest("Second", null, null, "uk", List.of()))
                .exchange().expectStatus().isOk();

        clientA.get().uri("/api/children").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    void should_create_multiple_children_in_one_batch_request() {
        String userA = seedUser();
        WebTestClient clientA = authedClient(userA);

        clientA.post().uri("/api/children/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateChildProfilesBatchRequest(List.of(
                        new CreateChildProfileRequest("Мія", (short) 2019, "girl", "uk", List.of("dragons")),
                        new CreateChildProfileRequest("Тарас", (short) 2017, "boy", "uk", List.of()),
                        new CreateChildProfileRequest("Sam", null, null, "en", List.of("space")))))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[0].name").isEqualTo("Мія")
                .jsonPath("$[2].name").isEqualTo("Sam");

        clientA.get().uri("/api/children").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(3);
    }

    @Test
    void should_reject_batch_with_invalid_child_with_400() {
        String userA = seedUser();
        WebTestClient clientA = authedClient(userA);

        clientA.post().uri("/api/children/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateChildProfilesBatchRequest(List.of(
                        new CreateChildProfileRequest("Valid", null, null, "uk", List.of()),
                        new CreateChildProfileRequest("  ", null, null, "uk", List.of()))))
                .exchange()
                .expectStatus().isBadRequest();

        // None should have been persisted for this user.
        clientA.get().uri("/api/children").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void should_return404_when_accessing_another_users_profile() {
        String userA = seedUser();
        String userB = seedUser();

        WebTestClient cA = authedClient(userA);
        String createdId = cA.post().uri("/api/children")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateChildProfileRequest("Owned", null, null, "uk", List.of()))
                .exchange().expectStatus().isOk()
                .returnResult(ChildProfileDto.class).getResponseBody().blockFirst().id();

        WebTestClient cB = authedClient(userB);
        cB.get().uri("/api/children/" + createdId).exchange().expectStatus().isNotFound();
        cB.patch().uri("/api/children/" + createdId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateChildProfileRequest("X", null, null, "uk", List.of()))
                .exchange().expectStatus().isNotFound();
        cB.delete().uri("/api/children/" + createdId).exchange().expectStatus().isNotFound();
    }

    private String seedUser() {
        String id = UUID.randomUUID().toString();
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@test.example");
        user.setDisplayName("Tester");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        users.save(user);
        return id;
    }

    /**
     * Obtains a Bearer access token via /api/auth/token/login and a CSRF token from
     * a safe GET, then returns a WebTestClient pre-configured with both.
     * The CSRF cookie+header pair is required for mutating requests (POST/PATCH/DELETE)
     * that are not in the CSRF exclusion list.
     */
    private WebTestClient authedClient(String userId) {
        String email = users.findById(userId).orElseThrow().getEmail();
        @SuppressWarnings("rawtypes")
        Map body = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123"))
                .exchange()
                .expectStatus().isOk()
                .returnResult(Map.class).getResponseBody().blockFirst();
        String bearer = body.get("accessToken").toString();

        // Fetch CSRF token from any safe GET endpoint
        EntityExchangeResult<byte[]> csrf = client()
                .get().uri("/api/billing/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult();
        MultiValueMap<String, ResponseCookie> csrfCookies = csrf.getResponseCookies();
        ResponseCookie xsrfCookie = csrfCookies.getFirst("XSRF-TOKEN");
        if (xsrfCookie == null) {
            throw new IllegalStateException("No XSRF-TOKEN cookie issued by server");
        }
        String csrfToken = xsrfCookie.getValue();

        return client().mutate()
                .defaultHeader("Authorization", "Bearer " + bearer)
                .defaultHeader("X-XSRF-TOKEN", csrfToken)
                .defaultCookie("XSRF-TOKEN", csrfToken)
                .build();
    }
}
