package com.kazka.billing.paypro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.billing.BillingProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PayProClientImplTest {

    private MockWebServer server;
    private PayProClientImpl client;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        BillingProperties.PayPro paypro =
                new BillingProperties.PayPro(123456, "secret-key", "ipn", false);
        BillingProperties props = new BillingProperties(
                null, null, null, null, null, null, null, null, null,
                paypro, null, null, null);
        client = new PayProClientImpl(props, server.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException { server.shutdown(); }

    @Test
    void should_postTerminate_withVendorAndKey() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"isSuccess\":true}"));

        StepVerifier.create(client.terminate("sub-99")).verifyComplete();

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(req).isNotNull();
        assertThat(req.getMethod()).isEqualTo("POST");
        assertThat(req.getPath()).isEqualTo("/api/Subscriptions/Terminate");
        JsonNode body = json.readTree(req.getBody().readUtf8());
        assertThat(body.get("vendorAccountId").asInt()).isEqualTo(123456);
        assertThat(body.get("apiSecretKey").asText()).isEqualTo("secret-key");
        assertThat(body.get("subscriptionId").asText()).isEqualTo("sub-99");
        assertThat(body.get("sendCustomerNotification").asBoolean()).isTrue();
        assertThat(body.get("reasonText").asText()).isEqualTo("User requested cancellation");
    }

    @Test
    void should_errorMono_when_payproReturnsNon2xx() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal error"));

        StepVerifier.create(client.terminate("sub-99"))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
