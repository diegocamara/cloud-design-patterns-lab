package org.example.orders.infrastructure.web.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderRequest(
    @NotNull UUID customerId, @NotNull @Positive BigDecimal totalAmount) {}
