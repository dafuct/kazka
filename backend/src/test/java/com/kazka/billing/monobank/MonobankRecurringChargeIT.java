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
        User user = newUser("e2e@example.com");
        SubscriptionProduct product = products.findByAppleProductId("kazka_pro_monthly").orElseThrow();
        product.setMonobankPlanId("mono_monthly_test");
        products.save(product);

        UserEntitlement entitlement = newMonobankEntitlement(user, product.getId(), 0);
        entitlements.save(entitlement);

        when(monobankClient.chargeToken(anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(new MonobankChargeResult.Accepted("inv_e2e")));

        service.tick();

        verify(monobankClient, times(1)).chargeToken(
                any(), any(), anyLong(), any(), any(), any());
        UserEntitlement after = entitlements.findById(entitlement.getId()).orElseThrow();
        assertThat(after.getRenewalRetryCount()).isZero();
        assertThat(after.getState()).isEqualTo(EntitlementState.ACTIVE);
    }

    @Test
    void should_beIdempotent_when_schedulerTicksTwice() {
        User idemUser = newUser("idem@example.com");
        SubscriptionProduct idemProduct = products.findByAppleProductId("kazka_pro_monthly").orElseThrow();
        idemProduct.setMonobankPlanId("mono_monthly_test");
        products.save(idemProduct);

        UserEntitlement idemEntitlement = newMonobankEntitlement(idemUser, idemProduct.getId(), 0);
        entitlements.save(idemEntitlement);

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
        UserEntitlement after = entitlements.findById(idemEntitlement.getId()).orElseThrow();
        assertThat(after.getRenewalRetryCount()).isZero();
    }

    private UserEntitlement newMonobankEntitlement(User user, String productId, int retries) {
        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setId(UUID.randomUUID().toString());
        entitlement.setUserId(user.getId());
        entitlement.setProductId(productId);
        entitlement.setSource(EntitlementSource.MONOBANK);
        entitlement.setState(EntitlementState.ACTIVE);
        entitlement.setExpiresAt(Instant.now().plus(Duration.ofDays(2)));
        entitlement.setNextRenewalAt(Instant.now().minusSeconds(60));
        entitlement.setMonobankWalletId("wallet-it");
        entitlement.setMonobankCardToken("card-it");
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
