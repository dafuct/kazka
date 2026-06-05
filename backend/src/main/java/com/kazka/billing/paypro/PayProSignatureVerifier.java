package com.kazka.billing.paypro;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Component
public class PayProSignatureVerifier {

    public boolean verify(Map<String, String> ipn, String validationKey) {
        if (validationKey == null || validationKey.isBlank()) return false;
        if (ipn == null) return false;
        String sig = ipn.get("SIGNATURE");
        if (sig == null || sig.isBlank()) return false;

        String input = nz(ipn.get("ORDER_ID"))
                + nz(ipn.get("ORDER_STATUS"))
                + nz(ipn.get("ORDER_TOTAL_AMOUNT"))
                + nz(ipn.get("CUSTOMER_EMAIL"))
                + validationKey
                + nz(ipn.get("TEST_MODE"))
                + nz(ipn.get("IPN_TYPE_NAME"));

        String expected;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            expected = HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                sig.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
