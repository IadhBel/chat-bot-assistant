package com.acme.assistant.api.dto;

import jakarta.validation.constraints.NotBlank;

public record IngestRequest(@NotBlank String source, @NotBlank String text) {
}
