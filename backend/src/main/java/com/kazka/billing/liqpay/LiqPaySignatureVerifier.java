package com.kazka.billing.liqpay;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class LiqPaySignatureVerifier {

    public boolean verify(String data, String signature, String privateKey) {
        if (data == null || signature == null || privateKey == null) return false;
        try {
            String concat = privateKey + data + privateKey;
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest(concat.getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getEncoder().encodeToString(digest);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    public String sign(String data, String privateKey) throws Exception {
        String concat = privateKey + data + privateKey;
        byte[] digest = MessageDigest.getInstance("SHA-1")
                .digest(concat.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(digest);
    }
}
