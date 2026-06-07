package com.kazka.billing;

import com.kazka.billing.dto.CheckoutSessionRequest;
import com.kazka.billing.dto.CheckoutSessionResponse;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutSessionServiceTest {

    @Mock SubscriptionProductRepository products;
    @Mock UserRepository users;
    @Mock CheckoutProvider monobankProvider;

    CheckoutSessionService service;

    private SubscriptionProduct product;
    private User user;

    @BeforeEach
    void setup() {
        product = new SubscriptionProduct();
        product.setId("prod-1");

        user = new User();
        user.setId("user-1");
        user.setEmail("u@example.com");

        lenient().when(products.findById("prod-1")).thenReturn(Optional.of(product));
        lenient().when(users.findById("user-1")).thenReturn(Optional.of(user));

        when(monobankProvider.provider()).thenReturn("monobank");

        service = new CheckoutSessionService(products, users,
                List.of(monobankProvider));
    }

    @Test
    void should_dispatchToMatchingProvider_when_providerIsMonobank() {
        CheckoutSessionResponse expected =
                new CheckoutSessionResponse("monobank", "https://pay.monobank.ua/invoice/xyz", null);
        when(monobankProvider.createSession(user, product)).thenReturn(Mono.just(expected));

        StepVerifier.create(service.create("user-1",
                        new CheckoutSessionRequest("prod-1", "monobank", "UA")))
                .expectNext(expected)
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
