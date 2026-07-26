package org.example.idempotency.infrastructure.web.model.response;

import java.util.UUID;

public record CreateTaskResponse(UUID id, String title) {}
