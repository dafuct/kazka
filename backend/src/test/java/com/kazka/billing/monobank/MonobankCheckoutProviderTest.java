package com.kazka.billing.monobank;

import com.kazka.billing.SubscriptionProduct;
import com.kazka.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonobankCheckoutProviderTest {

    @Mock MonobankClient monobank;
    @InjectMocks MonobankCheckoutProvider provider;

    @Test
    void should_haveProviderIdMonobank() {
        assertThat(provider.provider()).isEqualTo("monobank");
    }

    @Test
    void should_mapUrlToResponse_when_createSession() {
        SubscriptionProduct product = new SubscriptionProduct();
        product.setMonobankPlanId("mono_monthly");
        product.setPriceMicro(4_990_000L);
        product.setCurrency("USD");
        User user = new User();
        user.setId("user-1");

        when(monobank.createInvoiceUrl("mono_monthly", "user-1", 4_990_000L, "USD"))
                .thenReturn(Mono.just("https://pay.monobank.ua/invoice/xyz"));

        StepVerifier.create(provider.createSession(user, product))
                .assertNext(resp -> {
                    assertThat(resp.provider()).isEqualTo("monobank");
                    assertThat(resp.checkoutUrl()).contains("monobank");
                    assertThat(resp.paddleTransactionId()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void should_error_when_monobankPlanIdNotConfigured() {
        SubscriptionProduct product = new SubscriptionProduct();
        product.setPriceMicro(4_990_000L);
        product.setCurrency("USD");
        User user = new User();
        user.setId("user-1");

        StepVerifier.create(provider.createSession(user, product))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
