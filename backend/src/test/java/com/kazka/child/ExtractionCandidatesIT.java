package com.kazka.child;

import com.kazka.AbstractIT;
import com.kazka.ai.AiClient;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression for the "saved characters reappear after reload" bug: once the user confirms
 * characters for a tale (StoryCharacter join rows exist), the extraction-candidates endpoint
 * must stop re-offering them — otherwise every page reload re-derives and re-shows the panel.
 */
@Tag("integration")
@Import(ExtractionCandidatesIT.MockConfig.class)
class ExtractionCandidatesIT extends AbstractIT {

    @TestConfiguration
    static class MockConfig {
        @Bean @Primary
        AiClient mockAi() {
            AiClient aiClient = mock(AiClient.class);
            when(aiClient.streamText(anyString(), anyString())).thenReturn(Flux.just(
                    "[{\"name\":\"Мурчик\",\"kind\":\"animal\",\"description\":\"смішний кіт\","
                            + "\"traits\":[\"веселий\"],\"role\":\"companion\"}]"));
            return aiClient;
        }
    }

    @Autowired UserRepository users;
    @Autowired ChildProfileRepository profiles;
    @Autowired StoryRepository stories;
    @Autowired CharacterRepository characters;
    @Autowired StoryCharacterRepository storyCharacters;
    @Autowired PasswordEncoder passwordEncoder;

    String userId;
    String profileId;
    String storyId;

    @BeforeEach
    void seed() {
        storyCharacters.deleteAll();
        characters.deleteAll();
        stories.deleteAll();
        profiles.deleteAll();
        users.deleteAll();

        userId = seedUser();
        profileId = seedProfile(userId);
        storyId = seedStory(userId, profileId);
    }

    @AfterEach
    void tearDown() {
        storyCharacters.deleteAll();
        characters.deleteAll();
        stories.deleteAll();
        profiles.deleteAll();
        users.deleteAll();
    }

    @Test
    void offers_candidates_before_confirm_then_suppresses_them_after() {
        WebTestClient cli = authedClient(userId);

        // Before confirming: the panel gets its candidate.
        cli.get().uri("/api/stories/" + storyId + "/extraction-candidates?lang=uk")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Мурчик");

        // Simulate a confirm: a StoryCharacter join row now exists for this tale.
        String charId = seedCharacter(profileId);
        storyCharacters.save(new StoryCharacter(storyId, charId, "companion"));

        // After confirming: no candidates re-offered (panel stays gone across reloads).
        cli.get().uri("/api/stories/" + storyId + "/extraction-candidates?lang=uk")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    private String seedUser() {
        String id = UUID.randomUUID().toString();
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@test.example");
        user.setDisplayName("Tester");
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
        profile.setName("Лія");
        profile.setAvatarSeed("s");
        profile.setPreferredLanguage("uk");
        return profiles.save(profile).getId();
    }

    private String seedStory(String userId, String profileId) {
        Story story = new Story();
        story.setId(UUID.randomUUID().toString());
        story.setUserId(userId);
        story.setChildProfileId(profileId);
        story.setTitle("Казка");
        story.setTheme("пригода");
        story.setCharacters(List.of());
        story.setAgeGroup("3-5");
        story.setLength("short");
        story.setLanguage("uk");
        story.setContent("Жив-був смішний кіт Мурчик.");
        return stories.save(story).getId();
    }

    private String seedCharacter(String profileId) {
        Character character = new Character();
        character.setId(UUID.randomUUID().toString());
        character.setChildProfileId(profileId);
        character.setName("Мурчик");
        character.setKind("animal");
        character.setDescription("смішний кіт");
        character.setTraits(List.of("веселий"));
        characters.save(character);
        return character.getId();
    }

    private WebTestClient authedClient(String userId) {
        String email = users.findById(userId).orElseThrow().getEmail();
        @SuppressWarnings("rawtypes")
        Map body = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123"))
                .exchange()
                .expectStatus().isOk()
                .returnResult(Map.class).getResponseBody().blockFirst();
        String bearer = body.get("accessToken").toString();
        return client().mutate()
                .defaultHeader("Authorization", "Bearer " + bearer)
                .build();
    }
}
