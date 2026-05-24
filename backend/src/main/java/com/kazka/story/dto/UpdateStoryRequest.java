package com.kazka.story.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStoryRequest(
        @NotBlank String title,
        @NotBlank String content,
        String childProfileId
) {}
