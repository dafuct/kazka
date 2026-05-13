package com.kazka.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record IapVerifyRequest(@NotBlank String signedTransaction) {}
