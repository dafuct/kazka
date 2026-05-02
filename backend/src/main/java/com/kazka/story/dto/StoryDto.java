package com.kazka.story.dto;

import com.kazka.story.IllustrationStatus;
import com.kazka.story.Story;

import java.time.Instant;
import java.util.List;

public record StoryDto(
        String id,
        String title,
        String theme,
        List<String> characters,
        String ageGroup,
        String length,
        String language,
        String content,
        String illustrationPathLight,
        String illustrationPathDark,
        IllustrationStatus illustrationStatus,
        Instant createdAt,
        Instant updatedAt
) {
    public static StoryDto from(Story s) {
        return new StoryDto(
                s.getId(), s.getTitle(), s.getTheme(), s.getCharacters(),
                s.getAgeGroup(), s.getLength(), s.getLanguage(), s.getContent(),
                s.getIllustrationPathLight(), s.getIllustrationPathDark(),
                s.getIllustrationStatus(),
                s.getCreatedAt(), s.getUpdatedAt()
        );
    }
}
