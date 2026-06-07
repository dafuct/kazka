package com.kazka.billing.monobank;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.kazka.billing.BillingProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class MonobankClientImplTest {

    private WireMockServer mono;
    private MonobankClientImpl client;

    @BeforeEach
    void setUp() throws Exception {
        mono = new WireMockServer(wireMockConfig().dynamicPort());
        mono.start();

        BillingProperties.Monobank.Recurring recurring =
                new BillingProperties.Monobank.Recurring(Duration.ofHours(1), 3, "renew-");
        BillingProperties props = new BillingProperties(
                "bundle", "0", 0L, "Sandbox", "", "", "", false, 3,
                new BillingProperties.Monobank("tok", recurring),
                "http://localhost/success", "http://localhost/cancel"
        );

        client = new MonobankClientImpl(props);

        Field http = MonobankClientImpl.class.getDeclaredField("http");
        http.setAccessible(true);
        http.set(client, WebClient.builder()
                .baseUrl(mono.baseUrl())
                .defaultHeader("X-Token", "tok")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build());
    }

    @AfterEach
    void tearDown() {
        if (mono != null) {
            mono.stop();
        }
    }

    @Test
    void should_sendSaveCardTrue_when_createInvoiceUrl() {
        mono.stubFor(post(urlPathEqualTo("/api/merchant/invoice/create"))
                .withRequestBody(matchingJsonPath("$.saveCardData.saveCard", equalTo("true")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"pageUrl\":\"https://pay.mono.test/abc\",\"invoiceId\":\"INV-1\"}")));

        StepVerifier.create(client.createInvoiceUrl("pro", "user-1", 1_000_000L, "UAH"))
                .expectNext("https://pay.mono.test/abc")
                .verifyComplete();
    }

    @Test
    void should_returnAccepted_when_chargeToken_succeeds() {
        mono.stubFor(post(urlPathEqualTo("/api/merchant/wallet/payment"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"invoiceId\":\"INV-42\",\"status\":\"created\"}")));

        StepVerifier.create(client.chargeToken("wallet-1", "card-1", 1_000_000L, "UAH", "renew-abc", "ref-1"))
                .assertNext(r -> {
                    assertThat(r).isInstanceOf(MonobankChargeResult.Accepted.class);
                    assertThat(((MonobankChargeResult.Accepted) r).invoiceId()).isEqualTo("INV-42");
                })
                .verifyComplete();
    }

    @Test
    void should_returnCardFailure_when_chargeToken_4xx() {
        mono.stubFor(post(urlPathEqualTo("/api/merchant/wallet/payment"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errCode\":\"CARD_EXPIRED\"}")));

        StepVerifier.create(client.chargeToken("wallet-1", "card-1", 1_000_000L, "UAH", "renew-abc", "ref-1"))
                .assertNext(r -> assertThat(r).isInstanceOf(MonobankChargeResult.CardFailure.class))
                .verifyComplete();
    }

    @Test
    void should_returnTransient_when_chargeToken_5xx() {
        mono.stubFor(post(urlPathEqualTo("/api/merchant/wallet/payment"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errCode\":\"UPSTREAM_DOWN\"}")));

        StepVerifier.create(client.chargeToken("wallet-1", "card-1", 1_000_000L, "UAH", "renew-abc", "ref-1"))
                .assertNext(r -> assertThat(r).isInstanceOf(MonobankChargeResult.Transient.class))
                .verifyComplete();
    }

    @Test
    void should_callDeleteCard_when_deleteCard() {
        mono.stubFor(delete(urlPathEqualTo("/api/merchant/wallet/card"))
                .willReturn(aResponse().withStatus(200)));

        StepVerifier.create(client.deleteCard("wallet-1", "card-1"))
                .verifyComplete();

        mono.verify(com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor(
                        urlPathEqualTo("/api/merchant/wallet/card"))
                .withQueryParam("walletId", equalTo("wallet-1"))
                .withQueryParam("cardToken", equalTo("card-1")));
    }
}
