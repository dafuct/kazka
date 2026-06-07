package com.kazka.story.showcase;

import com.kazka.comics.PanelAspect;
import com.kazka.comics.StoryPanel;
import com.kazka.comics.StoryPanelRepository;
import com.kazka.config.UploadsProperties;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowcaseServiceTest {

    @Mock StoryRepository stories;
    @Mock StoryPanelRepository panels;
    @Mock UploadsProperties uploads;
    @InjectMocks ShowcaseService service;

    private Story showcaseStory(String id) {
        Story story = new Story();
        story.setId(id);
        story.setShowcase(true);
        story.setContent("Жили собі дід та баба.");
        return story;
    }

    @Test
    void should_list_showcase_tales() {
        when(stories.findAllByShowcaseTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(showcaseStory("s1")));
        when(panels.findByStoryIdOrderByPanelIndexAsc("s1")).thenReturn(List.of());

        List<ShowcaseStoryDto> result = service.list();

        assertThat(result).extracting(ShowcaseStoryDto::id).containsExactly("s1");
    }

    @Test
    void should_skip_showcase_tales_with_blank_content() {
        Story blank = showcaseStory("blank");
        blank.setContent("   ");
        when(stories.findAllByShowcaseTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(showcaseStory("s1"), blank));
        when(panels.findByStoryIdOrderByPanelIndexAsc("s1")).thenReturn(List.of());

        List<ShowcaseStoryDto> result = service.list();

        assertThat(result).extracting(ShowcaseStoryDto::id).containsExactly("s1");
    }

    @Test
    void should_rewrite_panel_image_urls_to_public_showcase_route() {
        StoryPanel panel = new StoryPanel();
        panel.setPanelIndex(1);
        panel.setImagePath("story-s1-page.png");
        panel.setNarration("n");
        panel.setAspect(PanelAspect.PAGE);
        when(stories.findAllByShowcaseTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(showcaseStory("s1")));
        when(panels.findByStoryIdOrderByPanelIndexAsc("s1")).thenReturn(List.of(panel));

        ShowcaseStoryDto dto = service.list().getFirst();

        assertThat(dto.panels()).hasSize(1);
        assertThat(dto.panels().getFirst().imageUrl())
                .isEqualTo("/api/public/showcase/s1/image/story-s1-page.png");
    }

    @Test
    void should_get_showcase_tale_by_id() {
        when(stories.findById("s1")).thenReturn(Optional.of(showcaseStory("s1")));
        when(panels.findByStoryIdOrderByPanelIndexAsc("s1")).thenReturn(List.of());

        ShowcaseStoryDto dto = service.get("s1");

        assertThat(dto.id()).isEqualTo("s1");
    }

    @Test
    void should_return_404_when_story_not_showcase() {
        Story notShowcase = showcaseStory("s1");
        notShowcase.setShowcase(false);
        when(stories.findById("s1")).thenReturn(Optional.of(notShowcase));

        assertThatThrownBy(() -> service.get("s1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void should_return_404_when_story_missing() {
        when(stories.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- resolveShowcaseImage ---------------------------------------------------------------

    @Test
    void should_resolve_image_path_for_showcase_panel(@TempDir Path tmp) throws Exception {
        Path image = tmp.resolve("img.png");
        Files.writeString(image, "png-bytes");
        when(stories.findById("s1")).thenReturn(Optional.of(showcaseStory("s1")));
        when(panels.existsByStoryIdAndImagePath("s1", "img.png")).thenReturn(true);
        when(uploads.getDir()).thenReturn(tmp.toString());

        Path resolved = service.resolveShowcaseImage("s1", "img.png");

        assertThat(resolved).isEqualTo(image.toAbsolutePath().normalize());
        assertThat(Files.exists(resolved)).isTrue();
    }

    @Test
    void should_resolve_image_path_when_uploads_dir_has_trailing_slash(@TempDir Path tmp) throws Exception {
        Files.writeString(tmp.resolve("img.png"), "png-bytes");
        when(stories.findById("s1")).thenReturn(Optional.of(showcaseStory("s1")));
        when(panels.existsByStoryIdAndImagePath("s1", "img.png")).thenReturn(true);
        when(uploads.getDir()).thenReturn(tmp + "/");

        Path resolved = service.resolveShowcaseImage("s1", "img.png");

        assertThat(resolved).isEqualTo(tmp.resolve("img.png").toAbsolutePath().normalize());
    }

    @Test
    void should_return_404_when_image_story_not_showcase() {
        Story notShowcase = showcaseStory("s1");
        notShowcase.setShowcase(false);
        when(stories.findById("s1")).thenReturn(Optional.of(notShowcase));

        assertThatThrownBy(() -> service.resolveShowcaseImage("s1", "img.png"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void should_return_404_when_image_story_missing() {
        when(stories.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveShowcaseImage("missing", "img.png"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void should_return_404_when_image_key_does_not_belong_to_story() {
        when(stories.findById("s1")).thenReturn(Optional.of(showcaseStory("s1")));
        when(panels.existsByStoryIdAndImagePath("s1", "img.png")).thenReturn(false);

        assertThatThrownBy(() -> service.resolveShowcaseImage("s1", "img.png"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void should_return_400_when_resolved_path_escapes_uploads_dir(@TempDir Path tmp) throws Exception {
        // A secret file one level above the uploads dir; the key tries to climb out to it.
        Path uploadsDir = tmp.resolve("uploads");
        Files.createDirectories(uploadsDir);
        Files.writeString(tmp.resolve("secret.png"), "top-secret");
        // The DB even says the key belongs to the story — containment must still block it,
        // proving traversal safety is independent of the ownership check.
        when(stories.findById("s1")).thenReturn(Optional.of(showcaseStory("s1")));
        when(panels.existsByStoryIdAndImagePath("s1", "../secret.png")).thenReturn(true);
        when(uploads.getDir()).thenReturn(uploadsDir.toString());

        assertThatThrownBy(() -> service.resolveShowcaseImage("s1", "../secret.png"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void should_return_404_when_resolved_image_file_is_missing(@TempDir Path tmp) {
        when(stories.findById("s1")).thenReturn(Optional.of(showcaseStory("s1")));
        when(panels.existsByStoryIdAndImagePath("s1", "img.png")).thenReturn(true);
        lenient().when(uploads.getDir()).thenReturn(tmp.toString());

        assertThatThrownBy(() -> service.resolveShowcaseImage("s1", "img.png"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
