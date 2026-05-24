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

    @Autowired
    com.kazka.billing.UserEntitlementRepository entitlementRepo;

    @BeforeEach
    void clean() {
        repository.deleteAll();
        entitlementRepo.deleteAll();
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
        Story s = new Story();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(seedUser());
        s.setTitle(title);
        s.setTheme("theme");
        s.setCharacters(List.of("Мія", "лисичка"));
        s.setAgeGroup("6-8");
        s.setLength("medium");
        s.setLanguage("uk");
        s.setContent("content");
        s.setIllustrationStatus(IllustrationStatus.PENDING);
        return s;
    }

    private String seedUser() {
        com.kazka.user.User u = new com.kazka.user.User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(u.getId() + "@example.com");
        u.setDisplayName("Test");
        u.setRole(com.kazka.user.UserRole.USER);
        u.setEmailVerified(true);
        userRepository.save(u);
        return u.getId();
    }
}
