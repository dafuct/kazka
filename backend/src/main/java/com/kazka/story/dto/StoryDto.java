package com.kazka.story.dto;

import com.kazka.child.ExtractionStatus;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.Story;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record StoryDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String theme,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> characters,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String ageGroup,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String length,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String language,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(nullable = true) String illustrationPathLight,
        @Schema(nullable = true) String illustrationPathDark,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) IllustrationStatus illustrationStatus,
        @Schema(nullable = true) String childProfileId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ExtractionStatus extractionStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean isBranching,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String branchingState,
        @Schema(nullable = true) List<com.kazka.story.branching.dto.BranchingChoice> pendingChoices,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
) {
    public static StoryDto from(Story s) {
        return new StoryDto(
                s.getId(), s.getTitle(), s.getTheme(), s.getCharacters(),
                s.getAgeGroup(), s.getLength(), s.getLanguage(), s.getContent(),
                s.getIllustrationPathLight(), s.getIllustrationPathDark(),
                s.getIllustrationStatus(),
                s.getChildProfileId(), s.getExtractionStatus(),
                s.isBranching(), s.getBranchingState(), s.getPendingChoices(),
                s.getCreatedAt(), s.getUpdatedAt()
        );
    }
}
