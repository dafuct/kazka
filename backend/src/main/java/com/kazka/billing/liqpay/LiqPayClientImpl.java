package com.kazka.billing.liqpay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.billing.BillingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "kazka.billing.liqpay", name = "public-key")
public class LiqPayClientImpl implements LiqPayClient {

    private final BillingProperties props;
    private final LiqPaySignatureVerifier sig;
    private final ObjectMapper json = new ObjectMapper();

    public LiqPayClientImpl(BillingProperties props, LiqPaySignatureVerifier sig) {
        this.props = props;
        this.sig = sig;
    }

    /**
     * LiqPay checkout uses a GET URL with two params: data (base64 JSON) and signature.
     * https://www.liqpay.ua/documentation/data_signature
     */
    @Override
    public Mono<String> createCheckoutUrl(String liqpayPlanId, String userId, long priceMicro, String currency) {
        return Mono.fromCallable(() -> {
            BillingProperties.LiqPay liqpay = props.liqpay();
            String publicKey = liqpay != null ? liqpay.publicKey() : "";
            String privateKey = liqpay != null ? liqpay.privateKey() : "";

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("public_key", publicKey);
            payload.put("version", 3);
            payload.put("action", "subscribe");
            payload.put("amount", priceMicro / 1_000_000.0);
            payload.put("currency", currency);
            payload.put("description", "Kazka Pro subscription");
            payload.put("order_id", liqpayPlanId + ":" + userId + ":" + System.currentTimeMillis());
            payload.put("subscribe", 1);
            payload.put("subscribe_date_start", "now");
            payload.put("subscribe_periodicity", "month");
            payload.put("result_url", props.successUrl());
            payload.put("server_url", baseUrl() + "/api/billing/webhook/liqpay");

            String data = Base64.getEncoder().encodeToString(
                    json.writeValueAsBytes(payload));
            String signature = sig.sign(data, privateKey);
            return "https://www.liqpay.ua/api/3/checkout"
                    + "?data=" + java.net.URLEncoder.encode(data, StandardCharsets.UTF_8)
                    + "&signature=" + java.net.URLEncoder.encode(signature, StandardCharsets.UTF_8);
        });
    }

    private String baseUrl() {
        // Derived from successUrl which has the host the frontend lives on; webhook host
        // is the backend, set via env var. For dev: same origin works.
        String s = props.successUrl();
        int idx = s.indexOf("/", 8);
        return idx > 0 ? s.substring(0, idx) : s;
    }
}
