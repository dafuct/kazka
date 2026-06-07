package com.kazka.story;

import com.kazka.AbstractIT;
import com.kazka.child.ExtractionStatus;
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
class StoryChildExtractionPersistenceIT extends AbstractIT {

    @Autowired StoryRepository repo;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        repo.deleteAll();
        users.deleteAll();
    }

    @Test
    void should_round_trip_extractionStatus_and_childProfileId() {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(user.getId() + "@test");
        user.setDisplayName("T");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        users.save(user);

        Story story = new Story();
        story.setId(UUID.randomUUID().toString());
        story.setUserId(user.getId());
        story.setTitle("t");
        story.setTheme("th");
        story.setCharacters(List.of("c"));
        story.setAgeGroup("6-8");
        story.setLength("short");
        story.setLanguage("uk");
        story.setContent("body");
        story.setExtractionStatus(ExtractionStatus.RUNNING);
        // child_profile_id stays null — column is nullable

        repo.save(story);

        Story loaded = repo.findById(story.getId()).orElseThrow();
        assertThat(loaded.getExtractionStatus()).isEqualTo(ExtractionStatus.RUNNING);
        assertThat(loaded.getChildProfileId()).isNull();
    }
}
