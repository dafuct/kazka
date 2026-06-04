package com.kazka.billing;

import com.apple.itunes.storekit.verification.VerificationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class IapVerifierTest {

    @Test
    void should_throw_when_billing_disabled_and_verify_called() {
        BillingProperties disabled = new BillingProperties(
                "app.kazka.ios", "n/a", 0L, "Sandbox", "i", "k", "", false, 3,
                null, null, null, null);
        IapVerifier v = new IapVerifier(disabled);
        assertThatThrownBy(() -> v.verifyTransaction("any"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not initialised");
    }

    @Test
    void should_throw_VerificationException_when_signed_data_is_garbage() throws Exception {
        BillingProperties enabled = new BillingProperties(
                "app.kazka.ios", "n/a", 1234567890L, "Sandbox", "i", "k", "", true, 3,
                null, null, null, null);
        IapVerifier v = new IapVerifier(enabled);
        v.init();
        assertThatThrownBy(() -> v.verifyTransaction("not.a.real.jws"))
                .isInstanceOf(VerificationException.class);
    }
}
