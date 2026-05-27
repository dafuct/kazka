package com.kazka.story.branching.dto;

import jakarta.validation.constraints.NotBlank;

public record BranchingChoiceRequest(@NotBlank String choiceId) {}
