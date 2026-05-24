package com.kazka.child;

import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
class CharacterRepositoryIT extends AbstractIT {

    @Autowired CharacterRepository repo;
    @Autowired ChildProfileRepository profiles;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void should_enforce_unique_name_per_profile() {
        String profileId = seedProfile();
        repo.saveAndFlush(newCharacter(profileId, "Мурка"));
        com.kazka.child.Character dup = newCharacter(profileId, "Мурка");
        assertThatThrownBy(() -> repo.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_allow_same_name_in_different_profiles() {
        String profileA = seedProfile();
        String profileB = seedProfile();
        repo.saveAndFlush(newCharacter(profileA, "Мурка"));
        repo.saveAndFlush(newCharacter(profileB, "Мурка"));
        assertThat(repo.count()).isGreaterThanOrEqualTo(2);
    }

    private com.kazka.child.Character newCharacter(String profileId, String name) {
        com.kazka.child.Character c = new com.kazka.child.Character();
        c.setId(UUID.randomUUID().toString());
        c.setChildProfileId(profileId);
        c.setName(name);
        c.setKind("animal");
        c.setDescription("a tortoiseshell cat with green eyes");
        c.setTraits(List.of("curious", "brave"));
        return c;
    }

    private String seedProfile() {
        String userId = UUID.randomUUID().toString();
        User u = new User();
        u.setId(userId);
        u.setEmail(userId + "@test");
        u.setDisplayName("T");
        u.setPasswordHash(passwordEncoder.encode("password123"));
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        users.save(u);

        ChildProfile p = new ChildProfile();
        p.setId(UUID.randomUUID().toString());
        p.setUserId(userId);
        p.setName("Test");
        p.setAvatarSeed("seed");
        p.setPreferredLanguage("uk");
        profiles.save(p);
        return p.getId();
    }
}
