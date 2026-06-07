package com.kazka.story.showcase;

import com.kazka.comics.StoryPanel;
import com.kazka.illustration.ImageUrlResolver;
import com.kazka.story.Story;
import com.kazka.story.dto.StoryPanelDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Minimal, public-safe projection of a {@link Story} for the unauthenticated showcase surface.
 *
 * <p>Deliberately exposes ONLY what an unregistered visitor needs to render the gallery and the
 * reader. Internal/ownership fields ({@code userId}, {@code childProfileId}), pipeline status
 * ({@code extractionStatus}, {@code illustrationStatus}), branching machinery
 * ({@code isBranching}, {@code branchingState}, {@code pendingChoices}), translation drafts
 * ({@code translatedContent}, {@code translatedLanguage}) and {@code updatedAt} are intentionally
 * omitted — unlike the authenticated {@link com.kazka.story.dto.StoryDto}.
 */
public record ShowcaseStoryDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String theme,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> characters,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String ageGroup,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String length,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String language,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<StoryPanelDto> panels,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt
) {
    public static ShowcaseStoryDto from(Story story, List<StoryPanel> panels, ImageUrlResolver resolver) {
        List<StoryPanelDto> panelDtos = panels == null
                ? List.of()
                : panels.stream().map(panel -> StoryPanelDto.from(panel, resolver)).toList();
        return new ShowcaseStoryDto(
                story.getId(), story.getTitle(), story.getTheme(), story.getCharacters(),
                story.getAgeGroup(), story.getLength(), story.getLanguage(), story.getContent(),
                panelDtos,
                story.getCreatedAt()
        );
    }
}
