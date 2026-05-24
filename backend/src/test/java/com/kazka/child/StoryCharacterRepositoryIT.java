package com.kazka.child;

import com.kazka.AbstractIT;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class StoryCharacterRepositoryIT extends AbstractIT {

    @Autowired StoryCharacterRepository joinRepo;
    @Autowired StoryRepository stories;
    @Autowired CharacterRepository characters;
    @Autowired ChildProfileRepository profiles;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void should_round_trip_story_character_relation() {
        String userId = seedUser();
        String profileId = seedProfile(userId);
        String storyId = seedStory(userId, profileId);
        String charId = seedCharacter(profileId);

        joinRepo.save(new StoryCharacter(storyId, charId, "protagonist"));

        List<StoryCharacter> byStory = joinRepo.findById_StoryId(storyId);
        assertThat(byStory).hasSize(1);
        assertThat(byStory.get(0).getRole()).isEqualTo("protagonist");
        assertThat(byStory.get(0).getId().getCharacterId()).isEqualTo(charId);
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
        p.setName("Test");
        p.setAvatarSeed("seed");
        p.setPreferredLanguage("uk");
        profiles.save(p);
        return p.getId();
    }

    private String seedStory(String userId, String profileId) {
        Story s = new Story();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(userId);
        s.setChildProfileId(profileId);
        s.setTitle("Test Story");
        s.setTheme("adventure");
        s.setCharacters(List.of());
        s.setAgeGroup("3-5");
        s.setLength("short");
        s.setLanguage("uk");
        s.setContent("Once upon a time...");
        stories.save(s);
        return s.getId();
    }

    private String seedCharacter(String profileId) {
        Character c = new Character();
        c.setId(UUID.randomUUID().toString());
        c.setChildProfileId(profileId);
        c.setName("Мурка-" + UUID.randomUUID());
        c.setKind("animal");
        c.setDescription("a tortoiseshell cat with green eyes");
        c.setTraits(List.of("curious", "brave"));
        characters.save(c);
        return c.getId();
    }
}
