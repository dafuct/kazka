package com.kazka.story.showcase;

import com.kazka.AbstractIT;
import com.kazka.comics.PanelAspect;
import com.kazka.comics.StoryPanel;
import com.kazka.comics.StoryPanelRepository;
import com.kazka.config.UploadsProperties;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage of the unauthenticated showcase image route. Writes a real PNG into the
 * application's configured uploads dir (discovered via {@link UploadsProperties}, so we do NOT
 * override any property — that would fork a new, non-cached Spring context and add another Hikari
 * pool, contributing to MySQL "Too many connections" across the full IT suite). It then asserts:
 * (a) happy path returns 200 with no auth,
 * (b) an ENCODED traversal key does NOT escape the uploads dir (4xx, not 200),
 * (c) a non-showcase story's image is 404.
 *
 * <p>All assertions are scoped to freshly generated UUIDs; rows and temp files are cleaned up in
 * {@code @AfterEach} — no global counts (see lesson it-global-count-assertion-fragile-to-shared-db).
 */
class ShowcaseImageRouteIT extends AbstractIT {

    @Autowired StoryRepository stories;
    @Autowired StoryPanelRepository panels;
    @Autowired UserRepository users;
    @Autowired UploadsProperties uploads;

    private final String ownerId = UUID.randomUUID().toString();
    private final String showcaseId = UUID.randomUUID().toString();
    private final String hiddenId = UUID.randomUUID().toString();
    private final String panelId = UUID.randomUUID().toString();
    private final String hiddenPanelId = UUID.randomUUID().toString();
    // Unique image file names so we never clobber real uploads sharing this dir.
    private final String imageKey = "showcase-it-" + UUID.randomUUID() + ".png";
    private final String hiddenImageKey = "hidden-it-" + UUID.randomUUID() + ".png";

    private Path uploadsDir;

    // Minimal valid 1x1 PNG.
    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
            0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
            0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
            0x42, 0x60, (byte) 0x82
    };

    @BeforeEach
    void seed() throws IOException {
        String dir = uploads.getDir().endsWith("/") ? uploads.getDir() : uploads.getDir() + "/";
        uploadsDir = Path.of(dir).toAbsolutePath().normalize();
        Files.createDirectories(uploadsDir);

        User owner = new User();
        owner.setId(ownerId);
        owner.setEmail(ownerId + "@example.com");
        owner.setDisplayName("Showcase Owner");
        owner.setRole(UserRole.USER);
        owner.setEmailVerified(true);
        users.save(owner);

        Files.write(uploadsDir.resolve(imageKey), PNG_BYTES);
        Files.write(uploadsDir.resolve(hiddenImageKey), PNG_BYTES);

        seedStory(showcaseId, true);
        seedStory(hiddenId, false);
        seedPanel(panelId, showcaseId, imageKey);
        seedPanel(hiddenPanelId, hiddenId, hiddenImageKey);
    }

    @AfterEach
    void cleanup() throws IOException {
        panels.deleteById(panelId);
        panels.deleteById(hiddenPanelId);
        stories.deleteById(showcaseId);
        stories.deleteById(hiddenId);
        users.deleteById(ownerId);
        if (uploadsDir != null) {
            Files.deleteIfExists(uploadsDir.resolve(imageKey));
            Files.deleteIfExists(uploadsDir.resolve(hiddenImageKey));
        }
    }

    private void seedStory(String id, boolean showcase) {
        Story story = new Story();
        story.setId(id);
        story.setUserId(ownerId);
        story.setTitle("Тестова казка " + id);
        story.setTheme("forest");
        story.setCharacters(List.of("fox"));
        story.setAgeGroup("3-5");
        story.setLength("short");
        story.setContent("Жили собі дід та баба.");
        story.setLanguage("uk");
        story.setShowcase(showcase);
        stories.save(story);
    }

    private void seedPanel(String id, String storyId, String key) {
        StoryPanel panel = new StoryPanel();
        panel.setId(id);
        panel.setStoryId(storyId);
        panel.setPanelIndex(1);
        panel.setImagePath(key);
        panel.setScenePrompt("a fox in a forest");
        panel.setNarration("narration");
        panel.setAspect(PanelAspect.PAGE);
        panels.save(panel);
    }

    @Test
    void should_stream_showcase_image_without_auth() {
        client().get().uri("/api/public/showcase/" + showcaseId + "/image/" + imageKey)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .consumeWith(result -> assertThat(result.getResponseBody()).isEqualTo(PNG_BYTES));
    }

    @Test
    void should_not_serve_file_outside_uploads_for_encoded_traversal_key() throws IOException {
        // A real secret one level above the uploads dir — the route must never reach it.
        Path secret = uploadsDir.getParent().resolve("kazka-it-secret-" + UUID.randomUUID() + ".png");
        Files.write(secret, "TOP-SECRET".getBytes());
        try {
            // Encoded "../<secret>.png": even decoded, it can never match a panel key and
            // (after normalization) never escapes the base dir.
            String encoded = "..%2F" + secret.getFileName();
            client().get().uri("/api/public/showcase/" + showcaseId + "/image/" + encoded)
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isGreaterThanOrEqualTo(400))
                    .expectBody().consumeWith(result -> {
                        // Whatever 4xx body comes back, it must NOT be the secret bytes.
                        byte[] body = result.getResponseBody();
                        if (body != null) {
                            assertThat(new String(body)).doesNotContain("TOP-SECRET");
                        }
                    });
        } finally {
            Files.deleteIfExists(secret);
        }
    }

    @Test
    void should_return_404_for_non_showcase_story_image() {
        client().get().uri("/api/public/showcase/" + hiddenId + "/image/" + hiddenImageKey)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }
}
