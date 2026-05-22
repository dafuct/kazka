package com.kazka.billing.monobank;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MonobankWebhookIT extends AbstractIT {

    private static final String SIGN_KEY = "shared_test_secret";

    @Autowired UserRepository users;
    @Autowired UserEntitlementRepository entitlements;
    @Autowired SubscriptionProductRepository products;
    @MockitoBean BillingProperties props;

    @BeforeEach
    void seed() {
        entitlements.deleteAll();
        users.deleteAll();
        when(props.monobank()).thenReturn(new BillingProperties.Monobank("tok", SIGN_KEY));
        SubscriptionProduct p = products.findByAppleProductId("kazka_pro_monthly").orElseThrow();
        p.setMonobankPlanId("mono_monthly_test");
        products.save(p);
    }

    @Test
    void should_writeActiveEntitlement_when_validSuccessWebhook() {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail("mono@example.com");
        u.setDisplayName("m");
        u.setPasswordHash("x");
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        users.save(u);

        String body = """
            {"invoiceId":"inv_1","status":"success",
             "reference":"mono_monthly_test:%s"}
            """.formatted(u.getId()).replaceAll("\\s+", " ");

        client().post().uri("/api/billing/webhook/monobank")
                .header("X-Sign", SIGN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();

        var rows = entitlements.findByUserId(u.getId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getState().name()).isEqualTo("ACTIVE");
        assertThat(rows.get(0).getSource().name()).isEqualTo("MONOBANK");
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
}
