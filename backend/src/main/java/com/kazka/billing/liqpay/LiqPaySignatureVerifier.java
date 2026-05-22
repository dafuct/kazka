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
            return constantTimeEquals(expected, signature);
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

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
