package com.kazka.child.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateChildProfilesBatchRequest(
        @NotEmpty @Size(max = 20) List<@Valid CreateChildProfileRequest> children
) {}
