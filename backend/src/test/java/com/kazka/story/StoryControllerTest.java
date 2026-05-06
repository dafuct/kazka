package com.kazka.story;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

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

    @BeforeEach
    void clearDatabase() {
        storyRepository.deleteAll();
    }

    @Test
    void getStories_withoutAuth_returns401() {
        webTestClient().get().uri("/api/stories")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getStory_withoutAuth_returns401() {
        webTestClient().get().uri("/api/stories/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isUnauthorized();
    }

}
