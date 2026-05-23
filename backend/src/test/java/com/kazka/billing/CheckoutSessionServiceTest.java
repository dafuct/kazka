package com.kazka.billing;

import com.kazka.billing.dto.CheckoutSessionRequest;
import com.kazka.billing.liqpay.LiqPayClient;
import com.kazka.billing.monobank.MonobankClient;
import com.kazka.billing.paddle.PaddleClient;
import com.kazka.billing.paddle.PaddleTransaction;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutSessionServiceTest {

    @Mock SubscriptionProductRepository products;
    @Mock UserRepository users;
    @Mock PaddleClient paddle;
    @Mock LiqPayClient liqpay;
    @Mock MonobankClient mono;
    @InjectMocks CheckoutSessionService service;

    private SubscriptionProduct product;
    private User user;

    @BeforeEach
    void setup() {
        product = new SubscriptionProduct();
        product.setId("prod-1");
        product.setAppleProductId("kazka_pro_monthly");
        product.setPaddleProductId("pro_P1M_real");
        product.setLiqpayPlanId("liqpay_monthly");
        product.setMonobankPlanId("mono_monthly");
        product.setPriceMicro(4_990_000L);
        product.setCurrency("USD");

        user = new User();
        user.setId("user-1");
        user.setEmail("u@example.com");

        lenient().when(products.findById("prod-1")).thenReturn(Optional.of(product));
        lenient().when(users.findById("user-1")).thenReturn(Optional.of(user));
    }

    @Test
    void should_returnPaddleCheckoutUrlAndTransactionId_when_providerIsPaddle() {
        when(paddle.createTransaction(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(new PaddleTransaction(
                        "txn_abc123",
                        "https://sandbox-checkout.paddle.com/checkout/txn_abc123")));

        StepVerifier.create(service.create("user-1",
                        new CheckoutSessionRequest("prod-1", "paddle", "DE")))
                .assertNext(resp -> {
                    org.assertj.core.api.Assertions.assertThat(resp.provider()).isEqualTo("paddle");
                    org.assertj.core.api.Assertions.assertThat(resp.paddleTransactionId()).isEqualTo("txn_abc123");
                    org.assertj.core.api.Assertions.assertThat(resp.checkoutUrl())
                            .isEqualTo("https://sandbox-checkout.paddle.com/checkout/txn_abc123");
                })
                .verifyComplete();
    }

    @Test
    void should_returnLiqPayCheckoutUrl_when_providerIsLiqpay() {
        when(liqpay.createCheckoutUrl(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(Mono.just("https://liqpay.ua/checkout/xyz"));

        StepVerifier.create(service.create("user-1",
                        new CheckoutSessionRequest("prod-1", "liqpay", "UA")))
                .assertNext(resp -> {
                    org.assertj.core.api.Assertions.assertThat(resp.provider()).isEqualTo("liqpay");
                    org.assertj.core.api.Assertions.assertThat(resp.checkoutUrl()).contains("liqpay.ua");
                    org.assertj.core.api.Assertions.assertThat(resp.paddleTransactionId()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void should_returnMonobankInvoiceUrl_when_providerIsMonobank() {
        when(mono.createInvoiceUrl(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(Mono.just("https://pay.monobank.ua/invoice/xyz"));

        StepVerifier.create(service.create("user-1",
                        new CheckoutSessionRequest("prod-1", "monobank", "UA")))
                .assertNext(resp -> {
                    org.assertj.core.api.Assertions.assertThat(resp.provider()).isEqualTo("monobank");
                    org.assertj.core.api.Assertions.assertThat(resp.checkoutUrl()).contains("monobank");
                })
                .verifyComplete();
    }

    @Test
    void should_errorOnUnknownProvider() {
        StepVerifier.create(service.create("user-1",
                        new CheckoutSessionRequest("prod-1", "btc", "UA")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
