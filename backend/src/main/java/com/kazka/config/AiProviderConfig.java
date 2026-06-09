package com.kazka.config;

import com.kazka.moderation.ModerationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiProviderConfig {

    @Bean
    public WebClient textClient(WebClient.Builder builder, AiProviderProperties aiProps) {
        return builder.clone()
                .baseUrl(aiProps.getTextBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, bearer(aiProps.getApiToken()))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    /**
     * Nano Banana (Gemini 2.5 Flash Image) native endpoint. Auth header is
     * {@code x-goog-api-key} — NOT {@code Authorization: Bearer}. The 20 MiB
     * in-memory cap is generous because Gemini returns the panel as a
     * base64-encoded {@code inline_data.data} field inline in the JSON body.
     */
    @Bean
    public WebClient nanoBananaWebClient(WebClient.Builder builder, AiProviderProperties aiProps) {
        return builder.clone()
                .baseUrl(aiProps.getNanoBananaBaseUrl())
                .defaultHeader("x-goog-api-key", aiProps.getApiToken() == null ? "" : aiProps.getApiToken())
                .codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();
    }

    /**
     * Gemini TTS native endpoint. Same v1beta base + x-goog-api-key as Nano Banana, but a larger
     * 32 MiB in-memory cap: Gemini returns the clip as base64 inline_data, and a few minutes of
     * 24 kHz/16-bit PCM base64-encodes past the 20 MiB image cap.
     */
    @Bean
    public WebClient geminiTtsWebClient(WebClient.Builder builder, AiProviderProperties aiProps) {
        return builder.clone()
                .baseUrl(aiProps.getNanoBananaBaseUrl())
                .defaultHeader("x-goog-api-key", aiProps.getApiToken() == null ? "" : aiProps.getApiToken())
                .codecs(c -> c.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))
                .build();
    }

    @Bean
    public WebClient judgeWebClient(WebClient.Builder builder,
                                    AiProviderProperties aiProps,
                                    ModerationProperties modProps) {
        return builder.clone()
                .baseUrl(modProps.getJudgeBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, bearer(aiProps.getApiToken()))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    private static String bearer(String token) {
        return "Bearer " + (token == null ? "" : token);
    }
}
