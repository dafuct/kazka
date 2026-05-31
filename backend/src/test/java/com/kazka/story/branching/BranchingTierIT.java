package com.kazka.story.branching;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementResolver;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.ai.AiClient;
import com.kazka.story.branching.dto.BranchingStartRequest;
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
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Tag("integration")
class BranchingTierIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired ChildProfileRepository profiles;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean EntitlementResolver entitlements;
    @MockitoBean AiClient aiClient;

    String freeUserId;
    String paidUserId;
    String freeProfileId;
    String paidProfileId;

    @BeforeEach
    void setup() {
        when(aiClient.streamText(anyString(), anyString())).thenReturn(
                Flux.just("Body.\n\n---\n\nCHOICE_A: A\nCHOICE_B: B"));
        freeUserId = seedUser();
        paidUserId = seedUser();
        freeProfileId = seedProfile(freeUserId);
        paidProfileId = seedProfile(paidUserId);
        when(entitlements.isPro(freeUserId)).thenReturn(false);
        when(entitlements.isPro(paidUserId)).thenReturn(true);
    }

    @Test
    void free_tier_gets_402() {
        authedClient(freeUserId).post().uri("/api/stories/branching")
                .bodyValue(new BranchingStartRequest("a", List.of("c"), "6-8", "short", "uk", freeProfileId, List.of()))
                .exchange()
                .expectStatus().isEqualTo(402);
    }

    @Test
    void paid_tier_gets_200() {
        authedClient(paidUserId).post().uri("/api/stories/branching")
                .bodyValue(new BranchingStartRequest("a", List.of("c"), "6-8", "short", "uk", paidProfileId, List.of()))
                .exchange()
                .expectStatus().isOk();
    }

    private String seedUser() {
        String id = UUID.randomUUID().toString();
        User u = new User();
        u.setId(id);
        u.setEmail(id + "@test.example");
        u.setDisplayName("Tester");
        u.setPasswordHash(passwordEncoder.encode("password123"));
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        users.save(u);
        return id;
    }

    private String seedProfile(String userId) {
        ChildProfile p = new ChildProfile();
        p.setId(UUID.randomUUID().toString());
        p.setUserId(userId);
        p.setName("Лія");
        p.setAvatarSeed("s");
        p.setPreferredLanguage("uk");
        return profiles.save(p).getId();
    }

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
