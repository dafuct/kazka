package com.kazka.billing.paddle;

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
class PaddleCheckoutProviderTest {

    @Mock PaddleClient paddle;
    @InjectMocks PaddleCheckoutProvider provider;

    @Test
    void should_haveProviderIdPaddle() {
        assertThat(provider.provider()).isEqualTo("paddle");
    }

    @Test
    void should_mapTransactionToResponse_when_createSession() {
        SubscriptionProduct product = new SubscriptionProduct();
        product.setPaddleProductId("pro_P1M_real");
        User user = new User();
        user.setId("user-1");
        user.setEmail("u@example.com");

        when(paddle.createTransaction("pro_P1M_real", "user-1", "u@example.com"))
                .thenReturn(Mono.just(new PaddleTransaction(
                        "txn_abc123", "https://sandbox-checkout.paddle.com/checkout/txn_abc123")));

        StepVerifier.create(provider.createSession(user, product))
                .assertNext(resp -> {
                    assertThat(resp.provider()).isEqualTo("paddle");
                    assertThat(resp.paddleTransactionId()).isEqualTo("txn_abc123");
                    assertThat(resp.checkoutUrl())
                            .isEqualTo("https://sandbox-checkout.paddle.com/checkout/txn_abc123");
                })
                .verifyComplete();
    }

    @Test
    void should_error_when_paddleProductIdNotConfigured() {
        SubscriptionProduct product = new SubscriptionProduct();
        User user = new User();
        user.setId("user-1");
        user.setEmail("u@example.com");

        StepVerifier.create(provider.createSession(user, product))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
