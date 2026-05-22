package com.kazka.billing.paddle;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Paddle Notification signing v1.
 * Header format: "ts=<unix>;h1=<hmac_sha256(ts + ":" + body, secret)>"
 */
@Component
public class PaddleSignatureVerifier {

    public boolean verify(String paddleSignatureHeader, String rawBody, String secret) {
        if (paddleSignatureHeader == null || secret == null) return false;
        String ts = null;
        String h1 = null;
        for (String part : paddleSignatureHeader.split(";")) {
            int eq = part.indexOf('=');
            if (eq < 0) continue;
            String k = part.substring(0, eq).trim();
            String v = part.substring(eq + 1).trim();
            if ("ts".equals(k)) ts = v;
            else if ("h1".equals(k)) h1 = v;
        }
        if (ts == null || h1 == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal((ts + ":" + rawBody).getBytes(StandardCharsets.UTF_8));
            String expectedHex = HexFormat.of().formatHex(expected);
            return constantTimeEquals(expectedHex, h1);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
