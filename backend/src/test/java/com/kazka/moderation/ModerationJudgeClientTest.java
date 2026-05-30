package com.kazka.moderation;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class ModerationJudgeClientTest {

    private WireMockServer wm;
    private ModerationJudgeClient client;

    @BeforeEach
    void start() {
        wm = new WireMockServer(options().dynamicPort());
        wm.start();
        ModerationProperties props = new ModerationProperties();
        props.setJudgeModel("Qwen/Qwen3-32B");
        props.setJudgeBaseUrl("http://localhost:" + wm.port());
        props.setJudgeTimeout(Duration.ofSeconds(2));
        WebClient webClient = WebClient.builder().baseUrl(props.getJudgeBaseUrl()).build();
        client = new ModerationJudgeClient(props, webClient);
    }

    @AfterEach
    void stop() { wm.stop(); }

    @Test
    void should_returnAllowed_when_judgeReturnsSafe() {
        stubGuard("safe");
        ModerationResult r = client.classify("uk", "пригоди трьох ведмежат", List.of("Sofia"));
        assertThat(r).isInstanceOf(ModerationResult.Allowed.class);
    }

    @Test
    void should_returnRefusedSexual_when_judgeReturnsUnsafeS1() {
        stubGuard("unsafe\nS1");
        ModerationResult r = client.classify("uk", "оголена принцеса", List.of());
        assertThat(r).isInstanceOf(ModerationResult.Refused.class);
        assertThat(((ModerationResult.Refused) r).category()).isEqualTo(ModerationCategory.SEXUAL);
    }

    @Test
    void should_returnRefusedDeath_when_judgeReturnsUnsafeS8() {
        stubGuard("unsafe\nS8");
        ModerationResult r = client.classify("en", "the dragon dies", List.of());
        assertThat(((ModerationResult.Refused) r).category()).isEqualTo(ModerationCategory.DEATH);
    }

    @Test
    void should_pickHighestSeverity_when_judgeReturnsMultipleCategories() {
        // S1 (Sexual) precedes S8 (Death) in severity ranking
        stubGuard("unsafe\nS8,S1");
        ModerationResult r = client.classify("en", "x", List.of());
        assertThat(((ModerationResult.Refused) r).category()).isEqualTo(ModerationCategory.SEXUAL);
    }

    @Test
    void should_returnJudgeUnavailable_when_judgeReturnsMalformed() {
        stubGuard("not-a-valid-response");
        ModerationResult r = client.classify("en", "x", List.of());
        assertThat(((ModerationResult.Refused) r).category()).isEqualTo(ModerationCategory.JUDGE_UNAVAILABLE);
    }

    @Test
    void should_returnJudgeUnavailable_when_judgeReturns500() {
        wm.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse().withStatus(500)));
        ModerationResult r = client.classify("en", "x", List.of());
        assertThat(((ModerationResult.Refused) r).category()).isEqualTo(ModerationCategory.JUDGE_UNAVAILABLE);
    }

    @Test
    void should_returnJudgeUnavailable_when_judgeTimesOut() {
        wm.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse().withFixedDelay(5_000).withStatus(200)
                        .withBody(chatJson("safe"))));
        ModerationResult r = client.classify("en", "x", List.of());
        assertThat(((ModerationResult.Refused) r).category()).isEqualTo(ModerationCategory.JUDGE_UNAVAILABLE);
    }

    @Test
    void should_findVerdict_when_judgePrefacesWithExplanation() {
        // Qwen instruct models sometimes emit a one-line preamble before the verdict.
        // The parser must scan all lines, not lock on lines[0].
        stubGuard("Assessment:\nunsafe\nS1");
        ModerationResult r = client.classify("en", "x", List.of());
        assertThat(((ModerationResult.Refused) r).category()).isEqualTo(ModerationCategory.SEXUAL);
    }

    @Test
    void should_findCategoriesAfterBlankLine_when_unsafeVerdictIsFollowedByEmptyLine() {
        stubGuard("unsafe\n\nS1,S3");
        ModerationResult r = client.classify("en", "x", List.of());
        assertThat(((ModerationResult.Refused) r).category()).isEqualTo(ModerationCategory.SEXUAL);
    }

    private void stubGuard(String content) {
        wm.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(chatJson(content))));
    }

    private static String chatJson(String content) {
        // OpenAI-style chat completion shape — same for HF/Gemini/OpenRouter.
        String escaped = content.replace("\"", "\\\"").replace("\n", "\\n");
        return """
            {"id":"x","choices":[{"message":{"role":"assistant","content":"%s"}}]}
            """.formatted(escaped);
    }
}
