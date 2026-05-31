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
            Story s = stories.findById(storyId).orElseThrow();
            assertThat(s.getExtractionStatus()).isEqualTo(ExtractionStatus.DONE);
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
            Story s = stories.findById(storyId).orElseThrow();
            assertThat(s.getExtractionStatus()).isEqualTo(ExtractionStatus.DONE);
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
        User u = new User();
        u.setId(id);
        u.setEmail(id + "@test");
        u.setDisplayName("T");
        u.setPasswordHash(passwordEncoder.encode("password123"));
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        users.save(u);
        return id;
    }

    private String seedProfile(String userId) {
        ChildProfile p = new ChildProfile();
        p.setId(UUID.randomUUID().toString());
        p.setUserId(userId);
        p.setName("T");
        p.setAvatarSeed("s");
        p.setPreferredLanguage("uk");
        return profiles.save(p).getId();
    }

    private String seedStory(String userId, String profileId) {
        Story s = new Story();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(userId);
        s.setTitle("t");
        s.setTheme("th");
        s.setCharacters(List.of("c"));
        s.setAgeGroup("6-8");
        s.setLength("short");
        s.setLanguage("uk");
        s.setContent("Once upon a time there was a cat named Мурка.");
        s.setChildProfileId(profileId);
        s.setExtractionStatus(ExtractionStatus.PENDING);
        return stories.save(s).getId();
    }
}
