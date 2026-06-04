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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonobankRecurringChargeIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired UserEntitlementRepository entitlements;
    @Autowired SubscriptionProductRepository products;
    @Autowired MonobankRecurringChargeService service;
    @MockitoBean MonobankClient monobankClient;

    @BeforeEach
    void seed() {
        entitlements.deleteAll();
        users.deleteAll();
    }

    @Test
    void should_chargeDueEntitlement_endToEnd() {
        User u = newUser("e2e@example.com");
        SubscriptionProduct p = products.findByAppleProductId("kazka_pro_monthly").orElseThrow();
        p.setMonobankPlanId("mono_monthly_test");
        products.save(p);

        UserEntitlement e = newMonobankEntitlement(u, p.getId(), 0);
        entitlements.save(e);

        when(monobankClient.chargeToken(anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(new MonobankChargeResult.Accepted("inv_e2e")));

        service.tick();

        verify(monobankClient, times(1)).chargeToken(
                any(), any(), anyLong(), any(), any(), any());
        UserEntitlement after = entitlements.findById(e.getId()).orElseThrow();
        assertThat(after.getRenewalRetryCount()).isZero();
        assertThat(after.getState()).isEqualTo(EntitlementState.ACTIVE);
    }

    @Test
    void should_beIdempotent_when_schedulerTicksTwice() {
        User u = newUser("idem@example.com");
        SubscriptionProduct p = products.findByAppleProductId("kazka_pro_monthly").orElseThrow();
        p.setMonobankPlanId("mono_monthly_test");
        products.save(p);

        UserEntitlement e = newMonobankEntitlement(u, p.getId(), 0);
        entitlements.save(e);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(monobankClient.chargeToken(anyString(), anyString(), anyLong(), anyString(),
                keyCaptor.capture(), anyString()))
                .thenReturn(Mono.just(new MonobankChargeResult.Accepted("inv_e2e")));

        service.tick();
        service.tick();

        // Idempotency key is {prefix}-{userId}-{yyyyMM}; two ticks in the same month emit the
        // same key. Monobank dedupes on its side; we only assert our local state stays sane.
        assertThat(keyCaptor.getAllValues()).hasSize(2);
        assertThat(keyCaptor.getAllValues().get(0)).isEqualTo(keyCaptor.getAllValues().get(1));
        UserEntitlement after = entitlements.findById(e.getId()).orElseThrow();
        assertThat(after.getRenewalRetryCount()).isZero();
    }

    private UserEntitlement newMonobankEntitlement(User u, String productId, int retries) {
        UserEntitlement e = new UserEntitlement();
        e.setId(UUID.randomUUID().toString());
        e.setUserId(u.getId());
        e.setProductId(productId);
        e.setSource(EntitlementSource.MONOBANK);
        e.setState(EntitlementState.ACTIVE);
        e.setExpiresAt(Instant.now().plus(Duration.ofDays(2)));
        e.setNextRenewalAt(Instant.now().minusSeconds(60));
        e.setMonobankWalletId("wallet-it");
        e.setMonobankCardToken("card-it");
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
