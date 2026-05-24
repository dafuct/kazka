package com.kazka.child.dto;

import java.util.List;

public record ExtractedCandidateDto(
        String name,
        String kind,
        String description,
        List<String> traits,
        String role
) {}
