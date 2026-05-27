package com.kazka.billing.gift;

import com.kazka.AbstractIT;
import com.kazka.billing.UserEntitlementRepository;
import com.kazka.billing.gift.dto.RedeemGiftRequest;
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
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class GiftRedemptionControllerIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired GiftCodeRepository codes;
    @Autowired UserEntitlementRepository entitlements;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanup() {
        codes.deleteAll();
    }

    @Test
    void happy_path_creates_active_entitlement() {
        String userId = seedUser();
        seedCode("HAPPY", 30, GiftCodeStatus.AVAILABLE);

        authedClient(userId).post().uri("/api/billing/gift/redeem")
                .bodyValue(new RedeemGiftRequest("HAPPY"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.durationDays").isEqualTo(30);

        assertThat(codes.findById("HAPPY").orElseThrow().getStatus())
                .isEqualTo(GiftCodeStatus.REDEEMED);
        assertThat(entitlements.findActiveByUserId(userId)).isPresent();
    }

    @Test
    void already_redeemed_returns_410() {
        String userId = seedUser();
        seedCode("USED", 30, GiftCodeStatus.REDEEMED);

        authedClient(userId).post().uri("/api/billing/gift/redeem")
                .bodyValue(new RedeemGiftRequest("USED"))
                .exchange()
                .expectStatus().isEqualTo(410);
    }

    @Test
    void unknown_code_returns_404() {
        String userId = seedUser();

        authedClient(userId).post().uri("/api/billing/gift/redeem")
                .bodyValue(new RedeemGiftRequest("NOSUCH"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void normalization_handles_lowercase_and_hyphens() {
        String userId = seedUser();
        seedCode("MIXED12", 7, GiftCodeStatus.AVAILABLE);

        authedClient(userId).post().uri("/api/billing/gift/redeem")
                .bodyValue(new RedeemGiftRequest("mixed-12"))
                .exchange()
                .expectStatus().isOk();
    }

    private void seedCode(String code, int days, GiftCodeStatus status) {
        GiftCode g = new GiftCode();
        g.setCode(code);
        g.setDurationDays(days);
        g.setStatus(status);
        codes.save(g);
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
        if (xsrfCookie == null) throw new IllegalStateException("No XSRF-TOKEN cookie");
        String csrfToken = xsrfCookie.getValue();

        return client().mutate()
                .defaultHeader("Authorization", "Bearer " + bearer)
                .defaultHeader("X-XSRF-TOKEN", csrfToken)
                .defaultCookie("XSRF-TOKEN", csrfToken)
                .build();
    }
}
