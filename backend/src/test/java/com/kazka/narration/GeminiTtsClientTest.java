package com.kazka.narration;

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

class GeminiTtsClientTest {

    private WireMockServer wm;
    private GeminiTtsClient client;
    private static final byte[] FAKE_PCM = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
    private static final String PATH = "/models/gemini-2.5-flash-preview-tts:generateContent";

    @BeforeEach
    void setUp() {
        wm = new WireMockServer(options().dynamicPort());
        wm.start();

        AiProviderProperties props = new AiProviderProperties();
        props.setApiToken("test-key");
        props.setTtsModel("gemini-2.5-flash-preview-tts");
        props.setTtsVoice("Sulafat");

        WebClient web = WebClient.builder()
                .baseUrl(wm.baseUrl())
                .defaultHeader("x-goog-api-key", "test-key")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))
                .build();

        client = new GeminiTtsClient(props, web, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        wm.stop();
    }

    @Test
    void should_returnPcmBytes_when_responseHasInlineAudio() {
        wm.stubFor(post(urlEqualTo(PATH)).willReturn(okJson(ttsResponse(FAKE_PCM))));

        byte[] result = client.synthesizePcm("Жила собі лисичка.", "Sulafat").block();

        assertThat(result).isEqualTo(FAKE_PCM);
    }

    @Test
    void should_sendVoiceAndAudioModality_when_synthesizing() {
        wm.stubFor(post(urlEqualTo(PATH)).willReturn(okJson(ttsResponse(FAKE_PCM))));

        client.synthesizePcm("текст", "Sulafat").block();

        wm.verify(postRequestedFor(urlEqualTo(PATH))
                .withRequestBody(containing("\"response_modalities\":[\"AUDIO\"]"))
                .withRequestBody(containing("\"voice_name\":\"Sulafat\""))
                .withRequestBody(containing("текст")));
    }

    @Test
    void should_throw_when_responseHasNoAudioPart() {
        wm.stubFor(post(urlEqualTo(PATH)).willReturn(okJson("""
            {"candidates":[{"content":{"parts":[{"text":"sorry"}]}}]}
            """)));

        assertThatThrownBy(() -> client.synthesizePcm("anything", "Sulafat").block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no inline audio");
    }

    private static String ttsResponse(byte[] pcm) {
        String b64 = Base64.getEncoder().encodeToString(pcm);
        return """
            {
              "candidates": [{
                "content": {
                  "parts": [
                    {"inline_data": {"mime_type": "audio/L16;rate=24000", "data": "%s"}}
                  ]
                }
              }]
            }
            """.formatted(b64);
    }
}
