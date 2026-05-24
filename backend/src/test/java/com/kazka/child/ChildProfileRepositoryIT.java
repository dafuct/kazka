package com.kazka.child;

import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class ChildProfileRepositoryIT extends AbstractIT {

    @Autowired ChildProfileRepository repo;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void should_persist_and_load_child_profile() {
        String userId = seedUser();
        ChildProfile p = newProfile(userId, "Лія");
        p.setBirthYear((short) 2020);
        p.setGender("girl");
        p.setInterests(List.of("dragons", "коти"));

        repo.save(p);
        ChildProfile loaded = repo.findById(p.getId()).orElseThrow();

        assertThat(loaded.getName()).isEqualTo("Лія");
        assertThat(loaded.getInterests()).containsExactly("dragons", "коти");
    }

    @Test
    void should_only_return_unarchived_profiles_for_user() {
        String userId = seedUser();
        ChildProfile active = newProfile(userId, "Active");
        ChildProfile archived = newProfile(userId, "Archived");
        archived.setArchivedAt(Instant.now());
        repo.save(active);
        repo.save(archived);

        List<ChildProfile> rows = repo.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc(userId);
        assertThat(rows).extracting(ChildProfile::getName).containsExactly("Active");
    }

    private ChildProfile newProfile(String userId, String name) {
        ChildProfile p = new ChildProfile();
        p.setId(UUID.randomUUID().toString());
        p.setUserId(userId);
        p.setName(name);
        p.setAvatarSeed("seed-" + name);
        p.setPreferredLanguage("uk");
        return p;
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
}
