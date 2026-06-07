package com.kazka.story.showcase;

import com.kazka.AbstractIT;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

class ShowcasePublicAccessIT extends AbstractIT {

    @Autowired StoryRepository stories;
    @Autowired UserRepository users;

    // Plain UUIDs (36 chars) — the id columns are length=36, so no prefix.
    private final String ownerId = UUID.randomUUID().toString();
    private final String showcaseId = UUID.randomUUID().toString();
    private final String hiddenId = UUID.randomUUID().toString();

    @BeforeEach
    void seedOwner() {
        User owner = new User();
        owner.setId(ownerId);
        owner.setEmail(ownerId + "@example.com");
        owner.setDisplayName("Showcase Owner");
        owner.setRole(UserRole.USER);
        owner.setEmailVerified(true);
        users.save(owner);
    }

    @AfterEach
    void cleanup() {
        stories.deleteById(showcaseId);
        stories.deleteById(hiddenId);
        // FK stories.user_id -> users.id ON DELETE CASCADE, but stories are
        // already gone above; delete the owner we created (scoped to this test).
        users.deleteById(ownerId);
    }

    private void seed(String id, boolean showcase) {
        Story story = new Story();
        story.setId(id);
        story.setUserId(ownerId);
        story.setTitle("Тестова казка " + id);
        story.setTheme("forest");
        story.setCharacters(java.util.List.of("fox"));
        story.setAgeGroup("3-5");
        story.setLength("short");
        story.setContent("Жили собі дід та баба.");
        story.setLanguage("uk");
        story.setShowcase(showcase);
        stories.save(story);
    }

    @Test
    void should_list_showcase_tale_without_auth() {
        seed(showcaseId, true);

        // No COOKIE / Authorization header — proves /api/public/** is permitAll.
        client().get().uri("/api/public/showcase")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // Scope the assertion to the id we created (shared-DB lesson:
                // never assert on a global count/order of all showcase rows).
                .jsonPath("$[?(@.id == '" + showcaseId + "')]").exists();
    }

    @Test
    void should_get_showcase_tale_without_auth() {
        seed(showcaseId, true);

        client().get().uri("/api/public/showcase/" + showcaseId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(showcaseId);
    }

    @Test
    void should_return_404_for_non_showcase_tale_without_auth() {
        seed(hiddenId, false);

        client().get().uri("/api/public/showcase/" + hiddenId)
                .exchange()
                .expectStatus().isNotFound();
    }
}
