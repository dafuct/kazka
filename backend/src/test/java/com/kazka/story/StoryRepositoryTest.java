package com.kazka.story;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StoryRepositoryTest {

    @Autowired
    StoryRepository repository;

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
}
