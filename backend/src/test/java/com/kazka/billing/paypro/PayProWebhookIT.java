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

        User user = newUser("paypro-it@example.com");
        userId = user.getId();

        SubscriptionProduct product = products.findByAppleProductId("kazka_pro_monthly").orElseThrow();
        product.setPayproProductId("44009");
        products.save(product);
        productId = product.getId();
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

        UserEntitlement entitlement = entitlements.findByOriginalTransactionId("sub-42").orElseThrow();
        assertThat(entitlement.getState()).isEqualTo(EntitlementState.ACTIVE);
        assertThat(entitlement.getSource()).isEqualTo(EntitlementSource.PAYPRO);
        assertThat(entitlement.getUserId()).isEqualTo(userId);
        assertThat(entitlement.getProductId()).isEqualTo(productId);
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

        UserEntitlement entitlement = entitlements.findByOriginalTransactionId("sub-42").orElseThrow();
        assertThat(entitlement.getState()).isEqualTo(EntitlementState.REVOKED);
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
        MultiValueMap<String, String> ipn = new LinkedMultiValueMap<>();
        ipn.set("ORDER_ID", "order-1");
        ipn.set("ORDER_STATUS", "Processed");
        ipn.set("ORDER_TOTAL_AMOUNT", "9.99");
        ipn.set("ORDER_CURRENCY_CODE", "USD");
        ipn.set("CUSTOMER_EMAIL", "paypro-it@example.com");
        ipn.set("CUSTOMER_ID", "cust-7");
        ipn.set("PRODUCT_ID", "44009");
        ipn.set("SUBSCRIPTION_ID", "sub-42");
        ipn.set("SUBSCRIPTION_NEXT_CHARGE_DATE", "2026-07-05T00:00:00Z");
        ipn.set("ORDER_CUSTOM_FIELDS", "x-kazka_user_id=" + userId + "&x-kazka_product_id=" + productId);
        ipn.set("TEST_MODE", "1");
        ipn.set("IPN_TYPE_ID", typeId);
        ipn.set("IPN_TYPE_NAME", typeName);
        return ipn;
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
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            ipn.set("SIGNATURE", HexFormat.of().formatHex(digest));
        } catch (Exception ex) { throw new RuntimeException(ex); }
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
