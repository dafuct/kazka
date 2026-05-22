package com.kazka.billing.paddle;

import com.kazka.AbstractIT;
import com.kazka.billing.BillingProperties;
import com.kazka.billing.SubscriptionProduct;
import com.kazka.billing.SubscriptionProductRepository;
import com.kazka.billing.UserEntitlementRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class PaddleWebhookIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired UserEntitlementRepository entitlements;
    @Autowired SubscriptionProductRepository products;
    @MockitoBean BillingProperties props;

    private static final String SECRET = "pdl_test_secret";

    @BeforeEach
    void seed() {
        entitlements.deleteAll();
        users.deleteAll();
        when(props.paddle()).thenReturn(new BillingProperties.Paddle("k", SECRET, "sandbox"));
        // Ensure the monthly product has a known paddleProductId
        SubscriptionProduct p = products.findByAppleProductId("kazka_pro_monthly").orElseThrow();
        p.setPaddleProductId("paddle_monthly_test");
        products.save(p);
    }

    @Test
    void should_writeActiveEntitlement_when_validTransactionCompletedEvent() throws Exception {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail("paddle@example.com");
        u.setDisplayName("p");
        u.setPasswordHash("x");
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        users.save(u);

        String body = """
            {"event_type":"transaction.completed",
             "data":{"id":"txn_abc",
                     "items":[{"price":{"id":"paddle_monthly_test"}}],
                     "custom_data":{"kazka_user_id":"%s"},
                     "next_billed_at":"2027-01-01T00:00:00Z"}}
            """.formatted(u.getId()).replaceAll("\\s+", " ");

        long ts = System.currentTimeMillis() / 1000;
        String sig = "ts=" + ts + ";h1=" + hmac(ts + ":" + body, SECRET);

        client().post().uri("/api/billing/webhook/paddle")
                .header("Paddle-Signature", sig)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();

        var rows = entitlements.findByUserId(u.getId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getState().name()).isEqualTo("ACTIVE");
        assertThat(rows.get(0).getSource().name()).isEqualTo("PADDLE");
    }

    @Test
    void should_returnOkButNotPersist_when_signatureInvalid() {
        client().post().uri("/api/billing/webhook/paddle")
                .header("Paddle-Signature", "ts=1;h1=bad")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();
        assertThat(entitlements.findAll()).isEmpty();
    }

    private static String hmac(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
