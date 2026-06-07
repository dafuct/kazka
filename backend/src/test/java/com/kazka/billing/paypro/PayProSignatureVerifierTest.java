package com.kazka.billing.paypro;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PayProSignatureVerifierTest {

    private final PayProSignatureVerifier v = new PayProSignatureVerifier();
    private final String key = "validation-key-from-paypro";

    @Test
    void should_acceptCorrectSignature() throws Exception {
        Map<String, String> ipn = sampleIpn();
        ipn.put("SIGNATURE", sha256Hex(
                ipn.get("ORDER_ID")
                + ipn.get("ORDER_STATUS")
                + ipn.get("ORDER_TOTAL_AMOUNT")
                + ipn.get("CUSTOMER_EMAIL")
                + key
                + ipn.get("TEST_MODE")
                + ipn.get("IPN_TYPE_NAME")
        ));
        assertThat(v.verify(ipn, key)).isTrue();
    }

    @Test
    void should_rejectWrongKey() throws Exception {
        Map<String, String> ipn = sampleIpn();
        ipn.put("SIGNATURE", sha256Hex(
                ipn.get("ORDER_ID")
                + ipn.get("ORDER_STATUS")
                + ipn.get("ORDER_TOTAL_AMOUNT")
                + ipn.get("CUSTOMER_EMAIL")
                + "wrong-key"
                + ipn.get("TEST_MODE")
                + ipn.get("IPN_TYPE_NAME")));
        assertThat(v.verify(ipn, key)).isFalse();
    }

    @Test
    void should_rejectMissingSignature() {
        Map<String, String> ipn = sampleIpn();
        // no SIGNATURE field
        assertThat(v.verify(ipn, key)).isFalse();
    }

    @Test
    void should_rejectBlankKey() {
        Map<String, String> ipn = sampleIpn();
        ipn.put("SIGNATURE", "anything");
        assertThat(v.verify(ipn, "")).isFalse();
        assertThat(v.verify(ipn, null)).isFalse();
    }

    @Test
    void should_treatMissingFieldAsEmptyString_inHash() throws Exception {
        Map<String, String> ipn = new HashMap<>();
        ipn.put("ORDER_ID", "100");
        ipn.put("CUSTOMER_EMAIL", "u@e.com");
        ipn.put("IPN_TYPE_NAME", "OrderCharged");
        // missing ORDER_STATUS, ORDER_TOTAL_AMOUNT, TEST_MODE — should be ""
        String expected = sha256Hex("100" + "" + "" + "u@e.com" + key + "" + "OrderCharged");
        ipn.put("SIGNATURE", expected);
        assertThat(v.verify(ipn, key)).isTrue();
    }

    private Map<String, String> sampleIpn() {
        Map<String, String> ipn = new HashMap<>();
        ipn.put("ORDER_ID", "12345");
        ipn.put("ORDER_STATUS", "Processed");
        ipn.put("ORDER_TOTAL_AMOUNT", "9.99");
        ipn.put("CUSTOMER_EMAIL", "user@example.com");
        ipn.put("TEST_MODE", "0");
        ipn.put("IPN_TYPE_NAME", "OrderCharged");
        return ipn;
    }

    private static String sha256Hex(String input) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
