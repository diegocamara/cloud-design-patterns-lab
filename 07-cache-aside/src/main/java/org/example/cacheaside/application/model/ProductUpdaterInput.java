package org.example.cacheaside.application.model;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductUpdaterInput(UUID id, BigDecimal price) {}
