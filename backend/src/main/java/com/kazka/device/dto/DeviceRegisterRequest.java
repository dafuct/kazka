package com.kazka.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DeviceRegisterRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String deviceToken,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"ios"}) @NotBlank @Pattern(regexp = "ios") String platform,
        @Schema(nullable = true) String locale
) {}
