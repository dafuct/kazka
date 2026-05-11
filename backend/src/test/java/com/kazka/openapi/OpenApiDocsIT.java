package com.kazka.openapi;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiDocsIT extends AbstractIT {

    @Test
    void should_returnOpenApi3Spec_when_apiDocsRequested() {
        client().get().uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.openapi").value((String s) -> assertThat(s).startsWith("3."))
                .jsonPath("$.info.title").exists();
    }

    @Test
    void should_includeAuthMePath_when_apiDocsRequested() {
        client().get().uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['/api/auth/me']").exists();
    }

    @Test
    void should_includeCoreDtoSchemas_when_apiDocsRequested() {
        client().get().uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.components.schemas.StoryDto").exists()
                .jsonPath("$.components.schemas.UserDto").exists();
    }
}
