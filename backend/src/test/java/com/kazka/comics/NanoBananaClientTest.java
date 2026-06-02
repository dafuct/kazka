package com.kazka.comics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.kazka.config.AiProviderProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NanoBananaClientTest {

    private WireMockServer wm;
    private NanoBananaClient client;
    private static final byte[] FAKE_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @BeforeEach
    void setUp() {
        wm = new WireMockServer(options().dynamicPort());
        wm.start();

        AiProviderProperties props = new AiProviderProperties();
        props.setApiToken("test-key");
        props.setNanoBananaModel("gemini-2.5-flash-image");
        props.setNanoBananaBaseUrl(wm.baseUrl());

        WebClient web = WebClient.builder()
                .baseUrl(wm.baseUrl())
                .defaultHeader("x-goog-api-key", "test-key")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();

        client = new NanoBananaClient(props, web, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        wm.stop();
    }

    @Test
    void should_returnImageBytes_when_responseHasInlineData() {
        wm.stubFor(post(urlEqualTo("/models/gemini-2.5-flash-image:generateContent"))
                .willReturn(okJson(geminiImageResponse(FAKE_PNG))));

        byte[] result = client.generate("a tiny fox in a starry forest", PanelAspect.LANDSCAPE, null).block();

        assertThat(result).isEqualTo(FAKE_PNG);
    }

    @Test
    void should_passPriorImageAsInlineData_when_chainingPanels() {
        wm.stubFor(post(urlEqualTo("/models/gemini-2.5-flash-image:generateContent"))
                .willReturn(okJson(geminiImageResponse(FAKE_PNG))));

        client.generate("panel 2", PanelAspect.SQUARE, FAKE_PNG).block();

        String expectedBase64 = Base64.getEncoder().encodeToString(FAKE_PNG);
        wm.verify(postRequestedFor(urlEqualTo("/models/gemini-2.5-flash-image:generateContent"))
                .withRequestBody(containing(expectedBase64))
                .withRequestBody(containing("\"aspect_ratio\":\"1:1\"")));
    }

    @Test
    void should_sendLandscapeAspect_when_panelIsLandscape() {
        wm.stubFor(post(urlEqualTo("/models/gemini-2.5-flash-image:generateContent"))
                .willReturn(okJson(geminiImageResponse(FAKE_PNG))));

        client.generate("panel 1", PanelAspect.LANDSCAPE, null).block();

        wm.verify(postRequestedFor(urlEqualTo("/models/gemini-2.5-flash-image:generateContent"))
                .withRequestBody(containing("\"aspect_ratio\":\"16:9\"")));
    }

    @Test
    void should_sendPageAspect_when_panelIsPage() {
        wm.stubFor(post(urlEqualTo("/models/gemini-2.5-flash-image:generateContent"))
                .willReturn(okJson(geminiImageResponse(FAKE_PNG))));

        client.generate("a full comic page", PanelAspect.PAGE, null).block();

        wm.verify(postRequestedFor(urlEqualTo("/models/gemini-2.5-flash-image:generateContent"))
                .withRequestBody(containing("\"aspect_ratio\":\"3:4\"")));
    }

    @Test
    void should_throw_when_responseHasNoImagePart() {
        wm.stubFor(post(urlEqualTo("/models/gemini-2.5-flash-image:generateContent"))
                .willReturn(okJson("""
                    {"candidates":[{"content":{"parts":[{"text":"sorry"}]}}]}
                    """)));

        assertThatThrownBy(() -> client.generate("anything", PanelAspect.LANDSCAPE, null).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no inline image");
    }

    private static String geminiImageResponse(byte[] png) {
        String b64 = Base64.getEncoder().encodeToString(png);
        return """
            {
              "candidates": [{
                "content": {
                  "parts": [
                    {"inline_data": {"mime_type": "image/png", "data": "%s"}}
                  ]
                }
              }]
            }
            """.formatted(b64);
    }
}
