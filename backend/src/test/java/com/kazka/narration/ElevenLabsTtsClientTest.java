package com.kazka.narration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.kazka.config.AiProviderProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ElevenLabsTtsClientTest {

    private WireMockServer wm;
    private ElevenLabsTtsClient client;
    private AiProviderProperties props;

    private static final byte[] FAKE_MP3 = {'I', 'D', '3', 0x04, 0x00, 0x11, 0x22, 0x33};
    private static final String UK_VOICE = "UkVoice123456789abcd";
    private static final String EN_VOICE = "EnVoice123456789abcd";
    private static final String UK_PATH = "/v1/text-to-speech/" + UK_VOICE;
    private static final String EN_PATH = "/v1/text-to-speech/" + EN_VOICE;

    @BeforeEach
    void setUp() {
        wm = new WireMockServer(options().dynamicPort());
        wm.start();

        props = new AiProviderProperties();
        AiProviderProperties.ElevenLabs el = props.getElevenlabs();
        el.setModel("eleven_multilingual_v2");
        el.setOutputFormat("mp3_44100_128");
        el.setApiKey("test-key");
        el.getVoices().put("uk", UK_VOICE);
        el.getVoices().put("en", EN_VOICE);

        WebClient web = WebClient.builder()
                .baseUrl(wm.baseUrl())
                .defaultHeader("xi-api-key", "test-key")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        client = new ElevenLabsTtsClient(props, web);
    }

    @AfterEach
    void tearDown() {
        wm.stop();
    }

    @Test
    void should_returnMp3Audio_when_apiReturnsBytes() {
        wm.stubFor(post(urlPathEqualTo(UK_PATH))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "audio/mpeg").withBody(FAKE_MP3)));

        TtsAudio audio = client.synthesize("Жила собі лисичка.", "uk").block();

        assertThat(audio).isNotNull();
        assertThat(audio.bytes()).isEqualTo(FAKE_MP3);
        assertThat(audio.contentType()).isEqualTo("audio/mpeg");
        assertThat(audio.fileExtension()).isEqualTo("mp3");
    }

    @Test
    void should_postToUkVoice_withModelSettingsAndFormat_when_languageUk() {
        wm.stubFor(post(urlPathEqualTo(UK_PATH)).willReturn(aResponse().withStatus(200).withBody(FAKE_MP3)));

        client.synthesize("текст казки", "uk").block();

        wm.verify(postRequestedFor(urlPathEqualTo(UK_PATH))
                .withQueryParam("output_format", equalTo("mp3_44100_128"))
                .withHeader("xi-api-key", equalTo("test-key"))
                .withRequestBody(containing("\"model_id\":\"eleven_multilingual_v2\""))
                .withRequestBody(containing("\"voice_settings\""))
                .withRequestBody(containing("\"stability\""))
                .withRequestBody(containing("\"use_speaker_boost\""))
                .withRequestBody(containing("текст казки")));
    }

    @Test
    void should_useEnVoice_when_languageEn() {
        wm.stubFor(post(urlPathEqualTo(EN_PATH)).willReturn(aResponse().withStatus(200).withBody(FAKE_MP3)));

        client.synthesize("Once upon a time.", "en").block();

        wm.verify(postRequestedFor(urlPathEqualTo(EN_PATH)));
    }

    @Test
    void should_fallBackToUkVoice_when_languageUnknown() {
        wm.stubFor(post(urlPathEqualTo(UK_PATH)).willReturn(aResponse().withStatus(200).withBody(FAKE_MP3)));

        client.synthesize("текст", "de").block();

        wm.verify(postRequestedFor(urlPathEqualTo(UK_PATH)));
    }

    @Test
    void should_errorWithoutHttpCall_when_noVoiceConfigured() {
        props.getElevenlabs().getVoices().clear();

        assertThatThrownBy(() -> client.synthesize("текст", "uk").block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("voice");

        wm.verify(0, postRequestedFor(urlPathMatching("/v1/text-to-speech/.*")));
    }
}
