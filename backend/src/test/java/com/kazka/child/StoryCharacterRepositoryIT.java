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
        profile.setName("Test");
        profile.setAvatarSeed("seed");
        profile.setPreferredLanguage("uk");
        profiles.save(profile);
        return profile.getId();
    }

    private String seedStory(String userId, String profileId) {
        Story story = new Story();
        story.setId(UUID.randomUUID().toString());
        story.setUserId(userId);
        story.setChildProfileId(profileId);
        story.setTitle("Test Story");
        story.setTheme("adventure");
        story.setCharacters(List.of());
        story.setAgeGroup("3-5");
        story.setLength("short");
        story.setLanguage("uk");
        story.setContent("Once upon a time...");
        stories.save(story);
        return story.getId();
    }

    private String seedCharacter(String profileId) {
        Character character = new Character();
        character.setId(UUID.randomUUID().toString());
        character.setChildProfileId(profileId);
        character.setName("Мурка-" + UUID.randomUUID());
        character.setKind("animal");
        character.setDescription("a tortoiseshell cat with green eyes");
        character.setTraits(List.of("curious", "brave"));
        characters.save(character);
        return character.getId();
    }
}
