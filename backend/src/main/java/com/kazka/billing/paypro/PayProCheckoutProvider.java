package com.kazka.billing.paypro;

import com.kazka.billing.CheckoutProvider;
import com.kazka.billing.SubscriptionProduct;
import com.kazka.billing.dto.CheckoutSessionResponse;
import com.kazka.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
public class PayProCheckoutProvider implements CheckoutProvider {

    private final PayProUrlBuilder urls;

    @Override
    public String provider() {
        return "paypro";
    }

    @Override
    public Mono<CheckoutSessionResponse> createSession(User user, SubscriptionProduct product) {
        return Mono.fromCallable(() -> {
            requireConfigured(product.getPayproProductId(), "payproProductId");
            String url = urls.build(user, product);
            return new CheckoutSessionResponse("paypro", url, null);
        });
    }
}
