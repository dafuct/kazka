package com.kazka.billing.paypro;

import com.kazka.billing.BillingProperties;
import com.kazka.billing.SubscriptionProduct;
import com.kazka.user.User;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class PayProUrlBuilder {

    private static final String BASE = "https://store.payproglobal.com/checkout";

    private final BillingProperties props;

    public PayProUrlBuilder(BillingProperties props) {
        this.props = props;
    }

    public String build(User user, SubscriptionProduct product) {
        List<String[]> params = new ArrayList<>();
        params.add(new String[]{"products[1][id]", product.getPayproProductId()});
        params.add(new String[]{"products[1][qty]", "1"});
        params.add(new String[]{"billing-email", user.getEmail()});
        params.add(new String[]{"currency", product.getCurrency()});
        params.add(new String[]{"x-kazka_user_id", user.getId()});
        params.add(new String[]{"x-kazka_product_id", product.getId()});
        BillingProperties.PayPro paypro = props.paypro();
        if (paypro != null && Boolean.TRUE.equals(paypro.useTestMode())) {
            params.add(new String[]{"use-test-mode", "true"});
        }

        StringBuilder sb = new StringBuilder(BASE).append('?');
        for (int index = 0; index < params.size(); index++) {
            if (index > 0) sb.append('&');
            // Keys are hard-coded constants with PayPro's PHP-style bracket syntax
            // (e.g. products[1][id]) — must be sent literal, not %5B/%5D-encoded.
            sb.append(params.get(index)[0]).append('=').append(encode(params.get(index)[1]));
        }
        return sb.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
