package com.kazka.billing.liqpay;

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
class LiqPayCheckoutProviderTest {

    @Mock LiqPayClient liqpay;
    @InjectMocks LiqPayCheckoutProvider provider;

    @Test
    void should_haveProviderIdLiqpay() {
        assertThat(provider.provider()).isEqualTo("liqpay");
    }

    @Test
    void should_mapUrlToResponse_when_createSession() {
        SubscriptionProduct product = new SubscriptionProduct();
        product.setLiqpayPlanId("liqpay_monthly");
        product.setPriceMicro(4_990_000L);
        product.setCurrency("USD");
        User user = new User();
        user.setId("user-1");

        when(liqpay.createCheckoutUrl("liqpay_monthly", "user-1", 4_990_000L, "USD"))
                .thenReturn(Mono.just("https://liqpay.ua/checkout/xyz"));

        StepVerifier.create(provider.createSession(user, product))
                .assertNext(resp -> {
                    assertThat(resp.provider()).isEqualTo("liqpay");
                    assertThat(resp.checkoutUrl()).contains("liqpay.ua");
                    assertThat(resp.paddleTransactionId()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void should_error_when_liqpayPlanIdNotConfigured() {
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
