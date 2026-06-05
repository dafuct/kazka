package com.kazka.billing.paypro;

import com.kazka.billing.BillingProperties;
import com.kazka.billing.SubscriptionProduct;
import com.kazka.user.User;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PayProUrlBuilderTest {

    private final BillingProperties props = new BillingProperties(
            null, null, null, null, null, null, null, null, null,
            new BillingProperties.PayPro(123456, "k", "s", true),
            null, null, null);

    private final PayProUrlBuilder builder = new PayProUrlBuilder(props);

    @Test
    void should_buildUrl_withAllRequiredParameters() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("u@example.com");

        SubscriptionProduct product = new SubscriptionProduct();
        product.setId("prod-1");
        product.setPayproProductId("44009");
        product.setCurrency("USD");

        String url = builder.build(user, product);

        assertThat(url).startsWith("https://store.payproglobal.com/checkout?");
        // Bracket keys must be sent literally — PayPro's checkout parses PHP-style notation
        // and %5B/%5D-encoded keys silently produce an empty cart.
        assertThat(url).contains("products[1][id]=44009");
        assertThat(url).contains("products[1][qty]=1");
        Map<String, String> params = parseQuery(url);
        assertThat(params).containsEntry("products[1][id]", "44009");
        assertThat(params).containsEntry("products[1][qty]", "1");
        assertThat(params).containsEntry("billing-email", "u@example.com");
        assertThat(params).containsEntry("currency", "USD");
        assertThat(params).containsEntry("x-kazka_user_id", "user-1");
        assertThat(params).containsEntry("x-kazka_product_id", "prod-1");
        assertThat(params).containsEntry("use-test-mode", "true");
    }

    @Test
    void should_omitTestModeParam_when_useTestModeFalse() {
        BillingProperties prodProps = new BillingProperties(
                null, null, null, null, null, null, null, null, null,
                new BillingProperties.PayPro(123456, "k", "s", false),
                null, null, null);
        PayProUrlBuilder live = new PayProUrlBuilder(prodProps);

        User user = new User();
        user.setId("u");
        user.setEmail("a@b.c");
        SubscriptionProduct product = new SubscriptionProduct();
        product.setId("p");
        product.setPayproProductId("1");
        product.setCurrency("USD");

        String url = live.build(user, product);

        assertThat(url).doesNotContain("use-test-mode");
    }

    @Test
    void should_urlEncodeUserIdContainingPlusSign() {
        User user = new User();
        user.setId("a+b/c=d");
        user.setEmail("plus+one@example.com");
        SubscriptionProduct product = new SubscriptionProduct();
        product.setId("p");
        product.setPayproProductId("1");
        product.setCurrency("USD");

        String url = builder.build(user, product);

        // Raw-wire check: + must be percent-encoded to %2B (else server reads it as space).
        assertThat(url).contains("x-kazka_user_id=a%2Bb%2Fc%3Dd");
        assertThat(url).contains("billing-email=plus%2Bone%40example.com");

        Map<String, String> params = parseQuery(url);
        assertThat(params).containsEntry("x-kazka_user_id", "a+b/c=d");
        assertThat(params).containsEntry("billing-email", "plus+one@example.com");
    }

    private Map<String, String> parseQuery(String url) {
        String q = URI.create(url).getRawQuery();
        Map<String, String> out = new HashMap<>();
        for (String pair : q.split("&")) {
            int eq = pair.indexOf('=');
            String k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            out.put(k, v);
        }
        return out;
    }
}
