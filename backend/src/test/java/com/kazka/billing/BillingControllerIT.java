package com.kazka.billing;

import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class BillingControllerIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired UserEntitlementRepository entitlements;
    @Autowired PasswordEncoder encoder;

    @MockitoBean IapVerifier verifier;

    @BeforeEach
    void clean() {
        entitlements.deleteAll();
        users.deleteAll();
    }

    @Test
    void should_listProducts_when_noAuth() {
        client().get().uri("/api/billing/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[?(@.appleProductId=='kazka_pro_monthly')].name").exists()
                .jsonPath("$[?(@.appleProductId=='kazka_pro_yearly')].name").exists();
    }

    @Test
    void should_return401_when_entitlementsAccessedWithoutAuth() {
        client().get().uri("/api/billing/entitlements")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void should_returnEmptyEntitlements_when_authedUserHasNoPurchases() {
        seedUser("empty-billing@example.com");
        String bearer = loginBearer("empty-billing@example.com");

        client().get().uri("/api/billing/entitlements")
                .header("Authorization", "Bearer " + bearer)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void should_persistEntitlement_when_verifyCalledWithValidJws() throws Exception {
        User seeded = seedUser("verify-billing@example.com");
        String bearer = loginBearer("verify-billing@example.com");

        JWSTransactionDecodedPayload payload = new JWSTransactionDecodedPayload();
        payload.setProductId("kazka_pro_monthly");
        payload.setOriginalTransactionId("987654321");
        payload.setExpiresDate(System.currentTimeMillis() + 30L * 24 * 3600 * 1000);
        when(verifier.verifyTransaction(anyString())).thenReturn(payload);

        client().post().uri("/api/billing/iap/verify")
                .header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("signedTransaction", "fake.jws.payload"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].productAppleId").isEqualTo("kazka_pro_monthly")
                .jsonPath("$[0].state").isEqualTo("ACTIVE");

        assertThat(entitlements.findByUserId(seeded.getId())).hasSize(1);
    }

    private User seedUser(String email) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode("password123"));
        user.setDisplayName("Tester");
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        return users.save(user);
    }

    private String loginBearer(String email) {
        @SuppressWarnings("rawtypes")
        Map body = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123"))
                .exchange().expectStatus().isOk()
                .returnResult(Map.class).getResponseBody().blockFirst();
        return body.get("accessToken").toString();
    }
}
