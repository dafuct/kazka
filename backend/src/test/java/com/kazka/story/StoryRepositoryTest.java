package com.kazka.story;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StoryRepositoryTest extends AbstractIT {

    @Autowired
    StoryRepository repository;

    @Autowired
    com.kazka.user.UserRepository userRepository;


    @BeforeEach
    void clean() {
        repository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void saveAndFindById() {
        Story story = story("Мія та лисичка");
        repository.save(story);

        Optional<Story> found = repository.findById(story.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Мія та лисичка");
        assertThat(found.get().getCharacters()).containsExactly("Мія", "лисичка");
        assertThat(found.get().getIllustrationStatus()).isEqualTo(IllustrationStatus.PENDING);
    }

    @Test
    void findAllByOrderByCreatedAtDesc_returnsPaginatedResults() {
        repository.save(story("A")); repository.save(story("B")); repository.save(story("C"));

        List<Story> all = repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10)).getContent();
        assertThat(all).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void deleteById_removesStory() {
        Story story = story("Delete me");
        repository.save(story);

        repository.deleteById(story.getId());

        assertThat(repository.findById(story.getId())).isEmpty();
    }

    private Story story(String title) {
        Story story = new Story();
        story.setId(UUID.randomUUID().toString());
        story.setUserId(seedUser());
        story.setTitle(title);
        story.setTheme("theme");
        story.setCharacters(List.of("Мія", "лисичка"));
        story.setAgeGroup("6-8");
        story.setLength("medium");
        story.setLanguage("uk");
        story.setContent("content");
        story.setIllustrationStatus(IllustrationStatus.PENDING);
        return story;
    }

    private String seedUser() {
        com.kazka.user.User user = new com.kazka.user.User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(user.getId() + "@example.com");
        user.setDisplayName("Test");
        user.setRole(com.kazka.user.UserRole.USER);
        user.setEmailVerified(true);
        userRepository.save(user);
        return user.getId();
    }
}
