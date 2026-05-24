package com.kazka.child.dto;

import com.kazka.child.Character;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record CharacterDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String childProfileId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"boy","girl","animal","creature","object"}) String kind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> traits,
        @Schema(nullable = true) String firstStoryId,
        @Schema(nullable = true) Instant lastUsedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int usageCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt
) {
    public static CharacterDto from(Character c) {
        return new CharacterDto(c.getId(), c.getChildProfileId(), c.getName(), c.getKind(),
                c.getDescription(), c.getTraits(), c.getFirstStoryId(), c.getLastUsedAt(),
                c.getUsageCount(), c.getCreatedAt());
    }
}
