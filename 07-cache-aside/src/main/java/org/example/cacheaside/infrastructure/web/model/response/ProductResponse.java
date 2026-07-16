package org.example.cacheaside.infrastructure.web.model.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(UUID id, String name, BigDecimal price) {}
