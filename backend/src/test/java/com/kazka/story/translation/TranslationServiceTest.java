package com.kazka.story.translation;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.ai.AiClient;
import com.kazka.comics.StoryPanelRepository;
import com.kazka.illustration.ImageUrlResolver;
import com.kazka.story.PromptBuilder;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.dto.StoryDto;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock StoryRepository stories;
    @Mock AiClient aiClient;
    @Mock TranslationPromptBuilder promptBuilder;
    @Mock PromptBuilder systemPromptBuilder;
    @Mock ImageUrlResolver images;
    @Mock StoryPanelRepository panelRepository;
    @InjectMocks TranslationService svc;

    private CurrentUser user() {
        return new CurrentUser("u1", UserRole.USER);
    }

    private Story story(String id, String userId, String language) {
        Story story = new Story();
        story.setId(id); story.setUserId(userId); story.setTitle("t"); story.setTheme("th");
        story.setContent("Жив-був дракон.");
        story.setLanguage(language);
        story.setBranching(false);
        story.setBranchingState("complete");
        return story;
    }

    @Test
    void translate_succeeds_for_any_user() {
        when(stories.findById("s1")).thenReturn(Optional.of(story("s1", "u1", "uk")));
        when(systemPromptBuilder.buildStorySystem("en")).thenReturn("system");
        when(promptBuilder.buildUserMessage("uk", "en", "Жив-був дракон.")).thenReturn("user");
        when(aiClient.streamText("system", "user")).thenReturn(Flux.just("Once upon a time, a dragon lived."));
        when(stories.save(any(Story.class))).thenAnswer(i -> i.getArgument(0));

        StoryDto dto = svc.translate("s1", "en", user()).block();

        assertThat(dto).isNotNull();
        assertThat(dto.translatedContent()).isEqualTo("Once upon a time, a dragon lived.");
        assertThat(dto.translatedLanguage()).isEqualTo("en");
    }

    @Test
    void translate_returns_404_when_story_not_owned() {
        when(stories.findById("s1")).thenReturn(Optional.of(story("s1", "other", "uk")));

        assertThatThrownBy(() -> svc.translate("s1", "en", user()).block())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void translate_returns_400_when_branching_incomplete() {
        Story incompleteStory = story("s1", "u1", "uk");
        incompleteStory.setBranching(true);
        incompleteStory.setBranchingState("awaiting_choice_1");
        when(stories.findById("s1")).thenReturn(Optional.of(incompleteStory));

        assertThatThrownBy(() -> svc.translate("s1", "en", user()).block())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void translate_returns_400_when_target_equals_source() {
        when(stories.findById("s1")).thenReturn(Optional.of(story("s1", "u1", "uk")));

        assertThatThrownBy(() -> svc.translate("s1", "uk", user()).block())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void translate_no_ops_when_already_translated_to_target() {
        Story alreadyTranslated = story("s1", "u1", "uk");
        alreadyTranslated.setTranslatedContent("Once upon a time…");
        alreadyTranslated.setTranslatedLanguage("en");
        when(stories.findById("s1")).thenReturn(Optional.of(alreadyTranslated));

        StoryDto dto = svc.translate("s1", "en", user()).block();

        assertThat(dto).isNotNull();
        assertThat(dto.translatedContent()).isEqualTo("Once upon a time…");
        verify(aiClient, never()).streamText(anyString(), anyString());
    }

    @Test
    void translate_happy_path_persists_translation() {
        when(stories.findById("s1")).thenReturn(Optional.of(story("s1", "u1", "uk")));
        when(systemPromptBuilder.buildStorySystem("en")).thenReturn("system");
        when(promptBuilder.buildUserMessage("uk", "en", "Жив-був дракон.")).thenReturn("user");
        when(aiClient.streamText("system", "user")).thenReturn(Flux.just("Once upon a time, a dragon lived."));
        when(stories.save(any(Story.class))).thenAnswer(i -> i.getArgument(0));

        StoryDto dto = svc.translate("s1", "en", user()).block();

        assertThat(dto).isNotNull();
        assertThat(dto.translatedContent()).isEqualTo("Once upon a time, a dragon lived.");
        assertThat(dto.translatedLanguage()).isEqualTo("en");
    }
}
