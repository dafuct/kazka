package com.kazka.billing.liqpay;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class LiqPayWebhookIT extends AbstractIT {

    private static final String PUB = "pub_test";
    private static final String PRIV = "priv_test";

    @Autowired UserRepository users;
    @Autowired UserEntitlementRepository entitlements;
    @Autowired SubscriptionProductRepository products;
    @MockitoBean BillingProperties props;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void seed() {
        entitlements.deleteAll();
        users.deleteAll();
        when(props.liqpay()).thenReturn(new BillingProperties.LiqPay(PUB, PRIV));
        SubscriptionProduct p = products.findByAppleProductId("kazka_pro_monthly").orElseThrow();
        p.setLiqpayPlanId("liqpay_monthly_test");
        products.save(p);
    }

    @Test
    void should_writeActiveEntitlement_when_validSubscribeCallback() throws Exception {
        User u = newUser("liqpay@example.com");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "success");
        payload.put("order_id", "liqpay_monthly_test:" + u.getId() + ":12345");
        payload.put("transaction_id", "lq_txn_1");
        String data = Base64.getEncoder().encodeToString(json.writeValueAsBytes(payload));
        String sig = liqSign(data, PRIV);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", data);
        form.add("signature", sig);

        client().post().uri("/api/billing/webhook/liqpay")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .exchange()
                .expectStatus().isOk();

        var rows = entitlements.findByUserId(u.getId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getState().name()).isEqualTo("ACTIVE");
        assertThat(rows.get(0).getSource().name()).isEqualTo("LIQPAY");
    }

    @Test
    void should_returnOkButNotPersist_when_signatureInvalid() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", "x");
        form.add("signature", "bad");

        client().post().uri("/api/billing/webhook/liqpay")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .exchange()
                .expectStatus().isOk();
        assertThat(entitlements.findAll()).isEmpty();
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

    private static String liqSign(String data, String key) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-1")
                .digest((key + data + key).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(d);
    }
}
