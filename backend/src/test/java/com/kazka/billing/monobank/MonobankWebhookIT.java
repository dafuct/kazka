package com.kazka.billing.monobank;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementSource;
import com.kazka.billing.EntitlementState;
import com.kazka.billing.SubscriptionProduct;
import com.kazka.billing.SubscriptionProductRepository;
import com.kazka.billing.UserEntitlement;
import com.kazka.billing.UserEntitlementRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MonobankWebhookIT extends AbstractIT {

    private static final KeyPair KEYS;
    static {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(new ECGenParameterSpec("secp256r1"));
            KEYS = g.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Autowired UserRepository users;
    @Autowired UserEntitlementRepository entitlements;
    @Autowired SubscriptionProductRepository products;
    @MockitoBean MonobankPubKeyService pubKeyService;

    @BeforeEach
    void seed() {
        entitlements.deleteAll();
        users.deleteAll();
        when(pubKeyService.publicKey()).thenReturn(Mono.just(KEYS.getPublic()));
        SubscriptionProduct p = products.findByAppleProductId("kazka_pro_monthly").orElseThrow();
        p.setMonobankPlanId("mono_monthly_test");
        products.save(p);
    }

    @Test
    void should_provisionEntitlementWithWalletAndToken_when_firstPaymentSuccess() {
        User u = newUser("mono1@example.com");

        String body = """
            {"invoiceId":"inv_1","status":"success",
             "reference":"mono_monthly_test:%s",
             "walletData":{"walletId":"wallet-xyz","cardToken":"card-abc","status":"created"}}
            """.formatted(u.getId()).replaceAll("\\s+", " ");

        client().post().uri("/api/billing/webhook/monobank")
                .header("X-Sign", sign(body))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();

        var rows = entitlements.findByUserId(u.getId());
        assertThat(rows).hasSize(1);
        UserEntitlement e = rows.get(0);
        assertThat(e.getState()).isEqualTo(EntitlementState.ACTIVE);
        assertThat(e.getSource()).isEqualTo(EntitlementSource.MONOBANK);
        assertThat(e.getMonobankWalletId()).isEqualTo("wallet-xyz");
        assertThat(e.getMonobankCardToken()).isEqualTo("card-abc");
        assertThat(e.getExpiresAt()).isNotNull().isAfter(Instant.now().plus(Duration.ofDays(29)));
        assertThat(e.getNextRenewalAt()).isNotNull().isBefore(e.getExpiresAt());
        assertThat(e.getRenewalRetryCount()).isZero();
    }

    @Test
    void should_extendExpiresAt_when_renewalWebhookSuccess() {
        User u = newUser("mono2@example.com");
        Instant originalExpires = Instant.now().plus(Duration.ofDays(1));
        UserEntitlement existing = newMonobankEntitlement(u, 0);
        existing.setExpiresAt(originalExpires);
        entitlements.save(existing);

        String body = """
            {"invoiceId":"inv_2","status":"success",
             "reference":"renew-%s-202606"}
            """.formatted(u.getId()).replaceAll("\\s+", " ");

        client().post().uri("/api/billing/webhook/monobank")
                .header("X-Sign", sign(body))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();

        UserEntitlement updated = entitlements.findById(existing.getId()).orElseThrow();
        assertThat(updated.getExpiresAt()).isAfter(originalExpires.plus(Duration.ofDays(29)));
        assertThat(updated.getRenewalRetryCount()).isZero();
        assertThat(updated.getNextRenewalAt()).isNotNull().isBefore(updated.getExpiresAt());
    }

    @Test
    void should_enterGrace_when_renewalWebhookFailure_andRetriesAvailable() {
        User u = newUser("mono3@example.com");
        UserEntitlement existing = newMonobankEntitlement(u, 0);
        entitlements.save(existing);

        String body = """
            {"invoiceId":"inv_3","status":"failure",
             "reference":"renew-%s-202606"}
            """.formatted(u.getId()).replaceAll("\\s+", " ");

        client().post().uri("/api/billing/webhook/monobank")
                .header("X-Sign", sign(body))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();

        UserEntitlement updated = entitlements.findById(existing.getId()).orElseThrow();
        assertThat(updated.getState()).isEqualTo(EntitlementState.ACTIVE);
        assertThat(updated.getRenewalRetryCount()).isEqualTo(1);
        assertThat(updated.getNextRenewalAt()).isAfter(Instant.now());
    }

    @Test
    void should_giveUpAndNullRenewal_when_renewalWebhookFailureAndRetriesExhausted() {
        User u = newUser("mono4@example.com");
        UserEntitlement existing = newMonobankEntitlement(u, 2);  // already retried twice
        entitlements.save(existing);

        String body = """
            {"invoiceId":"inv_4","status":"failure",
             "reference":"renew-%s-202606"}
            """.formatted(u.getId()).replaceAll("\\s+", " ");

        client().post().uri("/api/billing/webhook/monobank")
                .header("X-Sign", sign(body))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();

        UserEntitlement updated = entitlements.findById(existing.getId()).orElseThrow();
        assertThat(updated.getRenewalRetryCount()).isEqualTo(3);
        assertThat(updated.getNextRenewalAt()).isNull();
        assertThat(updated.getState()).isEqualTo(EntitlementState.ACTIVE);
    }

    @Test
    void should_returnOkButNotPersist_when_xSignMissing() {
        client().post().uri("/api/billing/webhook/monobank")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();
        assertThat(entitlements.findAll()).isEmpty();
    }

    @Test
    void should_returnOkButNotPersist_when_xSignDoesNotMatchBody() {
        User u = newUser("mono5@example.com");
        String body = """
            {"invoiceId":"inv_5","status":"success",
             "reference":"mono_monthly_test:%s"}
            """.formatted(u.getId()).replaceAll("\\s+", " ");
        String badSign = sign("{\"different\":\"body\"}");

        client().post().uri("/api/billing/webhook/monobank")
                .header("X-Sign", badSign)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();
        assertThat(entitlements.findByUserId(u.getId())).isEmpty();
    }

    private static String sign(String body) {
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(KEYS.getPrivate());
            s.update(body.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(s.sign());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private UserEntitlement newMonobankEntitlement(User u, int retries) {
        UserEntitlement e = new UserEntitlement();
        e.setId(UUID.randomUUID().toString());
        e.setUserId(u.getId());
        e.setProductId(products.findByAppleProductId("kazka_pro_monthly").orElseThrow().getId());
        e.setSource(EntitlementSource.MONOBANK);
        e.setState(EntitlementState.ACTIVE);
        e.setExpiresAt(Instant.now().plus(Duration.ofDays(2)));
        e.setNextRenewalAt(Instant.now().minusSeconds(60));
        e.setMonobankWalletId("wallet-xyz");
        e.setMonobankCardToken("card-abc");
        e.setRenewalRetryCount(retries);
        return e;
    }

    private User newUser(String email) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(email);
        u.setDisplayName("u");
        u.setPasswordHash("x");
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        return users.save(u);
    }
}
