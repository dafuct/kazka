package com.kazka.billing.monobank;

import com.kazka.billing.CheckoutProvider;
import com.kazka.billing.SubscriptionProduct;
import com.kazka.billing.dto.CheckoutSessionResponse;
import com.kazka.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
public class MonobankCheckoutProvider implements CheckoutProvider {

    private final MonobankClient monobank;

    @Override
    public String provider() {
        return "monobank";
    }

    @Override
    public Mono<CheckoutSessionResponse> createSession(User user, SubscriptionProduct product) {
        return Mono.defer(() -> monobank.createInvoiceUrl(
                        requireConfigured(product.getMonobankPlanId(), "monobankPlanId"),
                        user.getId(), product.getPriceMicro(), product.getCurrency()))
                .map(url -> new CheckoutSessionResponse("monobank", url, null));
    }
}
