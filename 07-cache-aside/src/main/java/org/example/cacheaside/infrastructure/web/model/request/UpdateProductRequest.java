package org.example.cacheaside.infrastructure.web.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateProductRequest(@NotNull @Positive BigDecimal price) {}
