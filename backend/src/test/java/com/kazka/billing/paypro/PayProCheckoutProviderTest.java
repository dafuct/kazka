package com.kazka.billing.paypro;

import com.kazka.billing.BillingProperties;
import com.kazka.billing.SubscriptionProduct;
import com.kazka.user.User;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class PayProCheckoutProviderTest {

    private final BillingProperties props = new BillingProperties(
            null, null, null, null, null, null, null, null, null,
            new BillingProperties.PayPro(1, "k", "s", true),
            null, null, null);

    private final PayProCheckoutProvider provider =
            new PayProCheckoutProvider(new PayProUrlBuilder(props));

    @Test
    void should_returnPayproChannel_withCheckoutUrl_andNullReference() {
        User user = new User();
        user.setId("u");
        user.setEmail("u@e.com");
        SubscriptionProduct product = new SubscriptionProduct();
        product.setId("p");
        product.setPayproProductId("44009");
        product.setCurrency("USD");

        StepVerifier.create(provider.createSession(user, product))
                .assertNext(resp -> {
                    assertThat(resp.provider()).isEqualTo("paypro");
                    assertThat(resp.checkoutUrl()).contains("44009");
                    assertThat(resp.providerReference()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void should_throw_when_productHasNoPayproId() {
        User user = new User();
        user.setId("u");
        user.setEmail("u@e.com");
        SubscriptionProduct product = new SubscriptionProduct();
        product.setId("p");
        product.setCurrency("USD");
        // payproProductId left null — guard must fire BEFORE URL build (which would NPE on null id)

        StepVerifier.create(provider.createSession(user, product))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
