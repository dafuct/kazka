package com.kazka.story;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StoryControllerTest {

    @LocalServerPort
    int port;

    WebTestClient webTestClient() {
        return WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Autowired
    StoryRepository storyRepository;

    @Test
    void getStories_returnsEmptyPage() {
        webTestClient().get().uri("/api/stories")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items").isArray()
                .jsonPath("$.total").isEqualTo(0);
    }

    @Test
    void getStory_notFound_returns404() {
        webTestClient().get().uri("/api/stories/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateStory_notFound_returns404() {
        webTestClient().put().uri("/api/stories/" + UUID.randomUUID())
                .bodyValue("""
                        {"title":"Updated","content":"New content"}
                        """)
                .header("Content-Type", "application/json")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteStory_notFound_returns404() {
        webTestClient().delete().uri("/api/stories/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void generate_invalidRequest_returns400() {
        webTestClient().post().uri("/api/stories/generate")
                .bodyValue("""
                        {"theme":"","characters":[],"ageGroup":"invalid","length":"x","language":"fr"}
                        """)
                .header("Content-Type", "application/json")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void saveAndGetStory_roundtrip() {
        Story story = new Story();
        story.setId(UUID.randomUUID().toString());
        story.setTitle("Test Story");
        story.setTheme("test");
        story.setCharacters(List.of("hero"));
        story.setAgeGroup("6-8");
        story.setLength("short");
        story.setLanguage("uk");
        story.setContent("Once upon a time...");
        story.setIllustrationStatus(IllustrationStatus.PENDING);
        storyRepository.save(story);

        webTestClient().get().uri("/api/stories/" + story.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Test Story")
                .jsonPath("$.illustrationStatus").isEqualTo("PENDING");
    }
}
