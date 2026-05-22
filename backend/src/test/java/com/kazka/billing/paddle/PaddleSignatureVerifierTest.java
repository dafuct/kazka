package com.kazka.billing.paddle;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class PaddleSignatureVerifierTest {

    private static final String SECRET = "pdl_ntfset_test_secret";

    @Test
    void should_returnTrue_when_signatureMatches() throws Exception {
        long ts = 1779000000L;
        String body = "{\"event_type\":\"transaction.completed\"}";
        String sig = "ts=" + ts + ";h1=" + hmac(ts + ":" + body, SECRET);
        PaddleSignatureVerifier v = new PaddleSignatureVerifier();
        assertThat(v.verify(sig, body, SECRET)).isTrue();
    }

    @Test
    void should_returnFalse_when_signatureMismatches() {
        PaddleSignatureVerifier v = new PaddleSignatureVerifier();
        assertThat(v.verify("ts=1;h1=deadbeef", "{}", SECRET)).isFalse();
    }

    @Test
    void should_returnFalse_when_headerMalformed() {
        PaddleSignatureVerifier v = new PaddleSignatureVerifier();
        assertThat(v.verify("garbage", "{}", SECRET)).isFalse();
        assertThat(v.verify(null, "{}", SECRET)).isFalse();
    }

    private static String hmac(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
