package com.kazka.child.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ConfirmCharactersRequest(
        @NotBlank String storyId,
        @NotEmpty @Size(max = 10) @Valid List<ExtractedCandidateDto> candidates
) {}
