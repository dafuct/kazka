package com.kazka.billing.paddle;

import com.kazka.billing.CheckoutProvider;
import com.kazka.billing.SubscriptionProduct;
import com.kazka.billing.dto.CheckoutSessionResponse;
import com.kazka.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
public class PaddleCheckoutProvider implements CheckoutProvider {

    private final PaddleClient paddle;

    @Override
    public String provider() {
        return "paddle";
    }

    @Override
    public Mono<CheckoutSessionResponse> createSession(User user, SubscriptionProduct product) {
        return Mono.defer(() -> paddle.createTransaction(
                        requireConfigured(product.getPaddleProductId(), "paddleProductId"),
                        user.getId(), user.getEmail()))
                .map(pt -> new CheckoutSessionResponse("paddle", pt.checkoutUrl(), pt.id()));
    }
}
