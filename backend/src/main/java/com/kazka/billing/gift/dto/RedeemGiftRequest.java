package com.kazka.billing.gift.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedeemGiftRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 4, max = 32) String code
) {}
