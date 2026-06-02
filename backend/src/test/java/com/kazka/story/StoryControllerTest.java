package com.kazka.story;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

class StoryControllerTest extends AbstractIT {

    @Autowired
    StoryRepository storyRepository;

    @BeforeEach
    void clearDatabase() {
        storyRepository.deleteAll();
    }

    @Test
    void getStories_withoutAuth_returns401() {
        client().get().uri("/api/stories")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getStory_withoutAuth_returns401() {
        client().get().uri("/api/stories/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getStoryStatus_withoutAuth_returns401() {
        client().get().uri("/api/stories/" + UUID.randomUUID() + "/status")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void retryStory_withoutAuth_isRejected() {
        // No bearer token => Spring Security rejects (401 from auth filter or 403 from CSRF
        // depending on filter order). Either way an unauthenticated POST must NOT reach the controller.
        client().post().uri("/api/stories/" + UUID.randomUUID() + "/retry")
                .exchange()
                .expectStatus().value(s -> {
                    if (s != 401 && s != 403) {
                        throw new AssertionError("expected 401 or 403, got " + s);
                    }
                });
    }
}
