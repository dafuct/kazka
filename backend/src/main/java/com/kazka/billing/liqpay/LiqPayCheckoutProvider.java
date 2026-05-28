package com.kazka.billing.liqpay;

import com.kazka.billing.CheckoutProvider;
import com.kazka.billing.SubscriptionProduct;
import com.kazka.billing.dto.CheckoutSessionResponse;
import com.kazka.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
public class LiqPayCheckoutProvider implements CheckoutProvider {

    private final LiqPayClient liqpay;

    @Override
    public String provider() {
        return "liqpay";
    }

    @Override
    public Mono<CheckoutSessionResponse> createSession(User user, SubscriptionProduct product) {
        return Mono.defer(() -> liqpay.createCheckoutUrl(
                        requireConfigured(product.getLiqpayPlanId(), "liqpayPlanId"),
                        user.getId(), product.getPriceMicro(), product.getCurrency()))
                .map(url -> new CheckoutSessionResponse("liqpay", url, null));
    }
}
