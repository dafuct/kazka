package com.kazka.admin;

import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowcaseAdminServiceTest {

    @Mock com.kazka.user.UserRepository users;
    @Mock StoryRepository stories;
    @InjectMocks AdminService service;

    @Test
    void should_set_showcase_true_when_toggled_on() {
        Story story = new Story();
        story.setId("s1");
        story.setShowcase(false);
        when(stories.findById("s1")).thenReturn(Optional.of(story));

        service.setShowcase("s1", true);

        ArgumentCaptor<Story> saved = ArgumentCaptor.forClass(Story.class);
        verify(stories).save(saved.capture());
        assertThat(saved.getValue().isShowcase()).isTrue();
    }

    @Test
    void should_throw_not_found_when_story_missing() {
        when(stories.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setShowcase("missing", true))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(stories, never()).save(any(Story.class));
    }
}
