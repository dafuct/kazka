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
}
