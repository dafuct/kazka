package com.kazka.billing.paypro;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementSource;
import com.kazka.billing.EntitlementState;
import com.kazka.billing.SubscriptionProduct;
import com.kazka.billing.SubscriptionProductRepository;
import com.kazka.billing.UserEntitlement;
import com.kazka.billing.UserEntitlementRepository;
import com.kazka.billing.webhook.ProcessedWebhookEventRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PayProWebhookIT extends AbstractIT {

    @Autowired SubscriptionProductRepository products;
    @Autowired UserRepository users;
    @Autowired UserEntitlementRepository entitlements;
    @Autowired ProcessedWebhookEventRepository processedEvents;

    private final String validationKey = "test-validation-key";  // matches application-test.yml

    private String userId;
    private String productId;

    @BeforeEach
    void seed() {
        entitlements.deleteAll();
        processedEvents.deleteAll();
        users.deleteAll();

        User u = newUser("paypro-it@example.com");
        userId = u.getId();

        SubscriptionProduct p = products.findByAppleProductId("kazka_pro_monthly").orElseThrow();
        p.setPayproProductId("44009");
        products.save(p);
        productId = p.getId();
    }

    @Test
    void should_grantActive_on_OrderCharged_validSignature() {
        MultiValueMap<String, String> ipn = baseIpn("OrderCharged", "1");
        sign(ipn);

        client().post().uri("/api/billing/webhook/paypro")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(ipn)
                .exchange()
                .expectStatus().isOk();

        UserEntitlement e = entitlements.findByOriginalTransactionId("sub-42").orElseThrow();
        assertThat(e.getState()).isEqualTo(EntitlementState.ACTIVE);
        assertThat(e.getSource()).isEqualTo(EntitlementSource.PAYPRO);
        assertThat(e.getUserId()).isEqualTo(userId);
        assertThat(e.getProductId()).isEqualTo(productId);
    }

    @Test
    void should_ignore_when_signatureMismatch() {
        MultiValueMap<String, String> ipn = baseIpn("OrderCharged", "1");
        ipn.set("SIGNATURE", "deadbeef");

        client().post().uri("/api/billing/webhook/paypro")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(ipn)
                .exchange()
                .expectStatus().isOk();

        assertThat(entitlements.findByOriginalTransactionId("sub-42")).isEmpty();
    }

    @Test
    void should_revoke_on_SubscriptionTerminated() {
        // Seed an ACTIVE row first via a prior OrderCharged.
        MultiValueMap<String, String> first = baseIpn("OrderCharged", "1");
        sign(first);
        client().post().uri("/api/billing/webhook/paypro")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(first).exchange().expectStatus().isOk();

        MultiValueMap<String, String> term = baseIpn("SubscriptionTerminated", "10");
        term.set("ORDER_ID", "order-2");
        sign(term);
        client().post().uri("/api/billing/webhook/paypro")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(term).exchange().expectStatus().isOk();

        UserEntitlement e = entitlements.findByOriginalTransactionId("sub-42").orElseThrow();
        assertThat(e.getState()).isEqualTo(EntitlementState.REVOKED);
    }

    @Test
    void should_dedupe_repeatDelivery() {
        MultiValueMap<String, String> ipn = baseIpn("OrderCharged", "1");
        sign(ipn);

        client().post().uri("/api/billing/webhook/paypro").contentType(MediaType.APPLICATION_FORM_URLENCODED).bodyValue(ipn).exchange().expectStatus().isOk();
        client().post().uri("/api/billing/webhook/paypro").contentType(MediaType.APPLICATION_FORM_URLENCODED).bodyValue(ipn).exchange().expectStatus().isOk();

        // Still exactly one entitlement row.
        assertThat(entitlements.findAll()).hasSize(1);
    }

    private MultiValueMap<String, String> baseIpn(String typeName, String typeId) {
        MultiValueMap<String, String> m = new LinkedMultiValueMap<>();
        m.set("ORDER_ID", "order-1");
        m.set("ORDER_STATUS", "Processed");
        m.set("ORDER_TOTAL_AMOUNT", "9.99");
        m.set("ORDER_CURRENCY_CODE", "USD");
        m.set("CUSTOMER_EMAIL", "paypro-it@example.com");
        m.set("CUSTOMER_ID", "cust-7");
        m.set("PRODUCT_ID", "44009");
        m.set("SUBSCRIPTION_ID", "sub-42");
        m.set("SUBSCRIPTION_NEXT_CHARGE_DATE", "2026-07-05T00:00:00Z");
        m.set("ORDER_CUSTOM_FIELDS", "x-kazka_user_id=" + userId + "&x-kazka_product_id=" + productId);
        m.set("TEST_MODE", "1");
        m.set("IPN_TYPE_ID", typeId);
        m.set("IPN_TYPE_NAME", typeName);
        return m;
    }

    private void sign(MultiValueMap<String, String> ipn) {
        try {
            String input = ipn.getFirst("ORDER_ID")
                    + ipn.getFirst("ORDER_STATUS")
                    + ipn.getFirst("ORDER_TOTAL_AMOUNT")
                    + ipn.getFirst("CUSTOMER_EMAIL")
                    + validationKey
                    + ipn.getFirst("TEST_MODE")
                    + ipn.getFirst("IPN_TYPE_NAME");
            byte[] h = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            ipn.set("SIGNATURE", HexFormat.of().formatHex(h));
        } catch (Exception ex) { throw new RuntimeException(ex); }
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
