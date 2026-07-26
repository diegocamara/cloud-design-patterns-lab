package org.example.idempotency.infrastructure.web.model.request;

import jakarta.validation.constraints.NotBlank;

public record CreateTaskRequest(@NotBlank String title) {}
