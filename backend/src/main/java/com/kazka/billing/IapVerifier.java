package com.kazka.billing;

import com.apple.itunes.storekit.model.Environment;
import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.apple.itunes.storekit.model.ResponseBodyV2DecodedPayload;
import com.apple.itunes.storekit.verification.SignedDataVerifier;
import com.apple.itunes.storekit.verification.VerificationException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Component
public class IapVerifier {

    private static final Logger log = LoggerFactory.getLogger(IapVerifier.class);

    private final BillingProperties props;
    private SignedDataVerifier verifier;

    public IapVerifier(BillingProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() throws IOException {
        if (!Boolean.TRUE.equals(props.enabled())) {
            log.info("Billing disabled — IapVerifier will reject all requests.");
            return;
        }
        try (InputStream is = new ClassPathResource("apple-root-ca-g3.cer").getInputStream()) {
            byte[] caBytes = is.readAllBytes();
            try (ByteArrayInputStream caStream = new ByteArrayInputStream(caBytes)) {
                this.verifier = new SignedDataVerifier(
                        Set.of(caStream),
                        props.bundleId(),
                        props.appleId(),
                        Environment.valueOf(props.environment().toUpperCase()),
                        /* enableOnlineChecks */ false
                );
            }
        }
    }

    public JWSTransactionDecodedPayload verifyTransaction(String signedTransaction) throws VerificationException {
        ensureReady();
        return verifier.verifyAndDecodeTransaction(signedTransaction);
    }

    public ResponseBodyV2DecodedPayload verifyNotification(String signedPayload) throws VerificationException {
        ensureReady();
        return verifier.verifyAndDecodeNotification(signedPayload);
    }

    private void ensureReady() {
        if (verifier == null) {
            throw new IllegalStateException("IapVerifier not initialised — billing disabled or root CA missing");
        }
    }
}
