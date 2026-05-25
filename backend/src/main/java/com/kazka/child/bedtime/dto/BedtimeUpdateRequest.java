package com.kazka.child.bedtime.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record BedtimeUpdateRequest(
        @NotNull Boolean enabled,
        @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String localTime,
        @NotBlank @Size(max = 50) String timezone,
        @Size(max = 10) List<@Size(max = 40) String> themes,
        @NotNull Boolean holidayThemesEnabled
) {}
