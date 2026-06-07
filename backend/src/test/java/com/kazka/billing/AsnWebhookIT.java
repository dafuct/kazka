package com.kazka.billing;

import com.apple.itunes.storekit.model.Data;
import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.apple.itunes.storekit.model.NotificationTypeV2;
import com.apple.itunes.storekit.model.ResponseBodyV2DecodedPayload;
import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AsnWebhookIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired SubscriptionProductRepository products;
    @Autowired UserEntitlementRepository entitlements;
    @Autowired PasswordEncoder encoder;

    @MockitoBean IapVerifier verifier;

    @BeforeEach
    void clean() {
        entitlements.deleteAll();
        users.deleteAll();
    }

    @Test
    void should_markEntitlementRefunded_when_REFUND_notificationArrives() throws Exception {
        User user = seedUser("asn-it@example.com");
        SubscriptionProduct product = products.findByAppleProductId("kazka_pro_monthly").orElseThrow();
        UserEntitlement entitlement = seedActiveEntitlement(user, product, "123456789");

        ResponseBodyV2DecodedPayload notif = new ResponseBodyV2DecodedPayload();
        notif.setNotificationType(NotificationTypeV2.REFUND);
        Data data = new Data();
        data.setSignedTransactionInfo("inner.signed.txn");
        notif.setData(data);
        when(verifier.verifyNotification(anyString())).thenReturn(notif);

        JWSTransactionDecodedPayload inner = new JWSTransactionDecodedPayload();
        inner.setOriginalTransactionId("123456789");
        inner.setProductId("kazka_pro_monthly");
        when(verifier.verifyTransaction(anyString())).thenReturn(inner);

        client().post().uri("/api/billing/iap/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("signedPayload", "fake.signed.payload"))
                .exchange()
                .expectStatus().is2xxSuccessful();

        UserEntitlement reloaded = entitlements.findById(entitlement.getId()).orElseThrow();
        assertThat(reloaded.getState()).isEqualTo(EntitlementState.REFUNDED);
    }

    private User seedUser(String email) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode("password123"));
        user.setDisplayName("Tester");
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        return users.save(user);
    }

    private UserEntitlement seedActiveEntitlement(User user, SubscriptionProduct product, String origTxn) {
        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setId(UUID.randomUUID().toString());
        entitlement.setUserId(user.getId());
        entitlement.setProductId(product.getId());
        entitlement.setState(EntitlementState.ACTIVE);
        entitlement.setOriginalTransactionId(origTxn);
        return entitlements.save(entitlement);
    }
}
