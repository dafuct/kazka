package com.kazka.child.dto;

import com.kazka.child.ChildProfile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record ChildProfileDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(nullable = true) Short birthYear,
        @Schema(nullable = true) String gender,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"uk","en","bilingual"}) String preferredLanguage,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> interests,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String avatarSeed,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long characterCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt
) {
    public static ChildProfileDto from(ChildProfile p, long characterCount) {
        return new ChildProfileDto(p.getId(), p.getName(), p.getBirthYear(), p.getGender(),
                p.getPreferredLanguage(), p.getInterests(), p.getAvatarSeed(),
                characterCount, p.getCreatedAt());
    }
}
