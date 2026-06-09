package com.kazka.story;

import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class StoryNarrationClaimIT extends AbstractIT {

    @Autowired StoryRepository stories;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    private String storyId;

    @BeforeEach
    void seed() {
        stories.deleteAll();
        users.deleteAll();
        User user = new User();
        String userId = UUID.randomUUID().toString();
        user.setId(userId);
        user.setEmail(userId + "@test.com");
        user.setDisplayName("Test");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        users.save(user);

        Story s = new Story();
        storyId = UUID.randomUUID().toString();
        s.setId(storyId);
        s.setUserId(userId);
        s.setTitle("t");
        s.setTheme("t");
        s.setCharacters(List.of("hero"));
        s.setAgeGroup("6-8");
        s.setLength("short");
        s.setLanguage("uk");
        s.setContent("Жила собі лисичка.");
        s.setIllustrationStatus(IllustrationStatus.PENDING);
        stories.save(s);
    }

    @Test
    void should_defaultNarrationStatusToNone_when_storySaved() {
        Story loaded = stories.findById(storyId).orElseThrow();
        assertThat(loaded.getNarrationStatus()).isEqualTo(NarrationStatus.NONE);
    }

    @Test
    void should_claimExactlyOnce_when_calledTwice() {
        int first = stories.claimNarration(storyId);
        int second = stories.claimNarration(storyId);
        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);
        assertThat(stories.findById(storyId).orElseThrow().getNarrationStatus())
                .isEqualTo(NarrationStatus.GENERATING);
    }

    @Test
    void should_setReadyWithKey_when_markNarrationReady() {
        stories.claimNarration(storyId);
        int updated = stories.markNarrationReady(storyId, "narration/" + storyId + ".wav");
        assertThat(updated).isEqualTo(1);
        Story loaded = stories.findById(storyId).orElseThrow();
        assertThat(loaded.getNarrationStatus()).isEqualTo(NarrationStatus.READY);
        assertThat(loaded.getNarrationKey()).isEqualTo("narration/" + storyId + ".wav");
    }

    @Test
    void should_setFailed_when_markNarrationFailed() {
        stories.claimNarration(storyId);
        int updated = stories.markNarrationFailed(storyId);
        assertThat(updated).isEqualTo(1);
        assertThat(stories.findById(storyId).orElseThrow().getNarrationStatus())
                .isEqualTo(NarrationStatus.FAILED);
    }

    @Test
    void should_claimAgain_when_previousAttemptFailed() {
        stories.claimNarration(storyId);
        stories.markNarrationFailed(storyId);
        int reclaim = stories.claimNarration(storyId);
        assertThat(reclaim).isEqualTo(1);
        assertThat(stories.findById(storyId).orElseThrow().getNarrationStatus())
                .isEqualTo(NarrationStatus.GENERATING);
    }
}
