package com.kazka.ollama;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

class OllamaClientTest {

    private WireMockServer wireMock;
    private OllamaClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        client = new OllamaClient(webClient);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void streamGenerate_emitsTokens() {
        wireMock.stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/x-ndjson")
                        .withBody("""
                                {"response":"Hello","done":false}
                                {"response":" world","done":false}
                                {"response":"","done":true}
                                """)));

        StepVerifier.create(client.streamGenerate("gemma3:4b", "Say hello"))
                .expectNext("Hello")
                .expectNext(" world")
                .verifyComplete();
    }

    @Test
    void generateImage_returnsBase64() {
        wireMock.stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"images":["aGVsbG8="]}
                                """)));

        StepVerifier.create(client.generateImage("x/flux2-klein", "A fairy tale scene"))
                .expectNext("aGVsbG8=")
                .verifyComplete();
    }

    @Test
    void streamGenerate_onError_propagatesError() {
        wireMock.stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse().withStatus(500)));

        StepVerifier.create(client.streamGenerate("gemma3:4b", "prompt"))
                .expectError()
                .verify();
    }

    @Test
    void generateImage_onError_returnsEmpty() {
        wireMock.stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse().withStatus(500)));

        StepVerifier.create(client.generateImage("x/flux2-klein", "prompt"))
                .verifyComplete();
    }
}
