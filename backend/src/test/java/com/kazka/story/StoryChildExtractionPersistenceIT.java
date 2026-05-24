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
    @Autowired com.kazka.billing.UserEntitlementRepository entitlementRepo;

    @BeforeEach
    void clean() {
        repo.deleteAll();
        entitlementRepo.deleteAll();
        users.deleteAll();
    }

    @Test
    void should_round_trip_extractionStatus_and_childProfileId() {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(u.getId() + "@test");
        u.setDisplayName("T");
        u.setPasswordHash(passwordEncoder.encode("password123"));
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        users.save(u);

        Story s = new Story();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(u.getId());
        s.setTitle("t");
        s.setTheme("th");
        s.setCharacters(List.of("c"));
        s.setAgeGroup("6-8");
        s.setLength("short");
        s.setLanguage("uk");
        s.setContent("body");
        s.setExtractionStatus(ExtractionStatus.RUNNING);
        // child_profile_id stays null — column is nullable

        repo.save(s);

        Story loaded = repo.findById(s.getId()).orElseThrow();
        assertThat(loaded.getExtractionStatus()).isEqualTo(ExtractionStatus.RUNNING);
        assertThat(loaded.getChildProfileId()).isNull();
    }
}
