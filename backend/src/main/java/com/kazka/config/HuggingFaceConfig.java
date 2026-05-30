package com.kazka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HuggingFaceConfig {

    @Bean
    public WebClient textClient(WebClient.Builder builder, HuggingFaceProperties huggingFaceProperties) {
        return builder.clone()
                .baseUrl(huggingFaceProperties.getTextBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, bearer(huggingFaceProperties.getApiToken()))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    /**
     * Fal.ai uses `Authorization: Key <FAL_KEY>` instead of `Bearer …`. Max payload is generous
     * because `sync_mode=true` returns the image as a base64 data URI inline in the JSON body.
     */
    @Bean
    public WebClient imageClient(WebClient.Builder builder, HuggingFaceProperties huggingFaceProperties) {
        return builder.clone()
                .baseUrl(huggingFaceProperties.getImageBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Key " + huggingFaceProperties.getImageApiToken())
                .codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();
    }

    @Bean
    public WebClient judgeWebClient(WebClient.Builder builder,
                                    HuggingFaceProperties hfProps,
                                    com.kazka.moderation.ModerationProperties modProps) {
        return builder.clone()
                .baseUrl(modProps.getJudgeBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, bearer(hfProps.getApiToken()))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    private static String bearer(String token) {
        return "Bearer " + (token == null ? "" : token);
    }
}
