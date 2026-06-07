package com.kazka.story.showcase;

import com.kazka.comics.StoryPanelRepository;
import com.kazka.config.UploadsProperties;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ShowcaseService {

    private final StoryRepository stories;
    private final StoryPanelRepository panels;
    private final UploadsProperties uploads;

    @Transactional(readOnly = true)
    public List<ShowcaseStoryDto> list() {
        return stories.findAllByShowcaseTrueOrderByCreatedAtDesc().stream()
                .filter(story -> story.getContent() != null && !story.getContent().isBlank())
                .map(this::toPublicDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShowcaseStoryDto get(String id) {
        Story story = stories.findById(id)
                .filter(Story::isShowcase)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toPublicDto(story);
    }

    /**
     * Resolves a requested showcase image key to an absolute file path, or throws a
     * {@link ResponseStatusException}. Validation lives here, behind a read-only transaction,
     * so the controller only streams.
     *
     * <p>Safety is layered and independent: the story must be a showcase (404), the key must
     * belong to one of its panels (404), and the resolved path must be contained within the
     * configured uploads directory (400). Containment via {@code startsWith} on normalized,
     * absolute paths is immune to encoded dots or absolute keys after URL decoding — it does
     * not rely on a brittle substring blocklist.
     */
    @Transactional(readOnly = true)
    public Path resolveShowcaseImage(String storyId, String key) {
        Story story = stories.findById(storyId).filter(Story::isShowcase)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!panels.existsByStoryIdAndImagePath(story.getId(), key)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String dir = uploads.getDir().endsWith("/") ? uploads.getDir() : uploads.getDir() + "/";
        Path base = Path.of(dir).toAbsolutePath().normalize();
        Path resolved = base.resolve(key).normalize();
        if (!resolved.startsWith(base)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (!Files.exists(resolved)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return resolved;
    }

    private ShowcaseStoryDto toPublicDto(Story story) {
        return ShowcaseStoryDto.from(story,
                panels.findByStoryIdOrderByPanelIndexAsc(story.getId()),
                new PublicImageUrlResolver(story.getId()));
    }
}
