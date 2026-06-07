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
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            KEYS = gen.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
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
        SubscriptionProduct product = products.findByAppleProductId("kazka_pro_monthly").orElseThrow();
        product.setMonobankPlanId("mono_monthly_test");
        products.save(product);
    }

    @Test
    void should_provisionEntitlementWithWalletAndToken_when_firstPaymentSuccess() {
        User user1 = newUser("mono1@example.com");

        String body = """
            {"invoiceId":"inv_1","status":"success",
             "reference":"mono_monthly_test:%s",
             "walletData":{"walletId":"wallet-xyz","cardToken":"card-abc","status":"created"}}
            """.formatted(user1.getId()).replaceAll("\\s+", " ");

        client().post().uri("/api/billing/webhook/monobank")
                .header("X-Sign", sign(body))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();

        var rows = entitlements.findByUserId(user1.getId());
        assertThat(rows).hasSize(1);
        UserEntitlement entitlement1 = rows.get(0);
        assertThat(entitlement1.getState()).isEqualTo(EntitlementState.ACTIVE);
        assertThat(entitlement1.getSource()).isEqualTo(EntitlementSource.MONOBANK);
        assertThat(entitlement1.getMonobankWalletId()).isEqualTo("wallet-xyz");
        assertThat(entitlement1.getMonobankCardToken()).isEqualTo("card-abc");
        assertThat(entitlement1.getExpiresAt()).isNotNull().isAfter(Instant.now().plus(Duration.ofDays(29)));
        assertThat(entitlement1.getNextRenewalAt()).isNotNull().isBefore(entitlement1.getExpiresAt());
        assertThat(entitlement1.getRenewalRetryCount()).isZero();
    }

    @Test
    void should_extendExpiresAt_when_renewalWebhookSuccess() {
        User user2 = newUser("mono2@example.com");
        Instant originalExpires = Instant.now().plus(Duration.ofDays(1));
        UserEntitlement existing = newMonobankEntitlement(user2, 0);
        existing.setExpiresAt(originalExpires);
        entitlements.save(existing);

        String body = """
            {"invoiceId":"inv_2","status":"success",
             "reference":"renew-%s-202606"}
            """.formatted(user2.getId()).replaceAll("\\s+", " ");

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
        User user3 = newUser("mono3@example.com");
        UserEntitlement existing = newMonobankEntitlement(user3, 0);
        entitlements.save(existing);

        String body = """
            {"invoiceId":"inv_3","status":"failure",
             "reference":"renew-%s-202606"}
            """.formatted(user3.getId()).replaceAll("\\s+", " ");

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
        User user4 = newUser("mono4@example.com");
        UserEntitlement existing = newMonobankEntitlement(user4, 2);  // already retried twice
        entitlements.save(existing);

        String body = """
            {"invoiceId":"inv_4","status":"failure",
             "reference":"renew-%s-202606"}
            """.formatted(user4.getId()).replaceAll("\\s+", " ");

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
        User user5 = newUser("mono5@example.com");
        String body = """
            {"invoiceId":"inv_5","status":"success",
             "reference":"mono_monthly_test:%s"}
            """.formatted(user5.getId()).replaceAll("\\s+", " ");
        String badSign = sign("{\"different\":\"body\"}");

        client().post().uri("/api/billing/webhook/monobank")
                .header("X-Sign", badSign)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();
        assertThat(entitlements.findByUserId(user5.getId())).isEmpty();
    }

    private static String sign(String body) {
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(KEYS.getPrivate());
            sig.update(body.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private UserEntitlement newMonobankEntitlement(User user, int retries) {
        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setId(UUID.randomUUID().toString());
        entitlement.setUserId(user.getId());
        entitlement.setProductId(products.findByAppleProductId("kazka_pro_monthly").orElseThrow().getId());
        entitlement.setSource(EntitlementSource.MONOBANK);
        entitlement.setState(EntitlementState.ACTIVE);
        entitlement.setExpiresAt(Instant.now().plus(Duration.ofDays(2)));
        entitlement.setNextRenewalAt(Instant.now().minusSeconds(60));
        entitlement.setMonobankWalletId("wallet-xyz");
        entitlement.setMonobankCardToken("card-abc");
        entitlement.setRenewalRetryCount(retries);
        return entitlement;
    }

    private User newUser(String email) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setDisplayName("u");
        user.setPasswordHash("x");
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        return users.save(user);
    }
}
