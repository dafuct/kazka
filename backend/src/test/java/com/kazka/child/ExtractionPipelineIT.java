package com.kazka.child;

import com.kazka.AbstractIT;
import com.kazka.ai.AiClient;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Tag("integration")
class ExtractionPipelineIT extends AbstractIT {

    @Autowired CharacterExtractionWorker worker;
    @Autowired StoryRepository stories;
    @Autowired ChildProfileRepository profiles;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean AiClient aiClient;

    @Test
    void should_transition_pending_running_done_on_happy_path() {
        when(aiClient.streamText(anyString(), anyString())).thenReturn(Flux.just(
                "[{\"name\":\"Мурка\",\"kind\":\"animal\",\"description\":\"a cat\",\"traits\":[\"curious\"],\"role\":\"companion\"}]"));

        String userId = seedUser();
        String profileId = seedProfile(userId);
        String storyId = seedStory(userId, profileId);

        worker.enqueueAsync(storyId).join();

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Story story = stories.findById(storyId).orElseThrow();
            assertThat(story.getExtractionStatus()).isEqualTo(ExtractionStatus.DONE);
        });
    }

    @Test
    void should_mark_done_with_empty_extraction_when_LLM_returns_empty_array() {
        when(aiClient.streamText(anyString(), anyString())).thenReturn(Flux.just("[]"));

        String userId = seedUser();
        String profileId = seedProfile(userId);
        String storyId = seedStory(userId, profileId);

        worker.enqueueAsync(storyId).join();

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Story story = stories.findById(storyId).orElseThrow();
            assertThat(story.getExtractionStatus()).isEqualTo(ExtractionStatus.DONE);
        });
    }

    @Test
    void should_skip_silently_when_story_missing() {
        // race vs DELETE: worker is called with a non-existent ID
        worker.run(UUID.randomUUID().toString());
        // No exception thrown; worker logs and exits
    }

    private String seedUser() {
        String id = UUID.randomUUID().toString();
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@test");
        user.setDisplayName("T");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        users.save(user);
        return id;
    }

    private String seedProfile(String userId) {
        ChildProfile profile = new ChildProfile();
        profile.setId(UUID.randomUUID().toString());
        profile.setUserId(userId);
        profile.setName("T");
        profile.setAvatarSeed("s");
        profile.setPreferredLanguage("uk");
        return profiles.save(profile).getId();
    }

    private String seedStory(String userId, String profileId) {
        Story story = new Story();
        story.setId(UUID.randomUUID().toString());
        story.setUserId(userId);
        story.setTitle("t");
        story.setTheme("th");
        story.setCharacters(List.of("c"));
        story.setAgeGroup("6-8");
        story.setLength("short");
        story.setLanguage("uk");
        story.setContent("Once upon a time there was a cat named Мурка.");
        story.setChildProfileId(profileId);
        story.setExtractionStatus(ExtractionStatus.PENDING);
        return stories.save(story).getId();
    }
}
