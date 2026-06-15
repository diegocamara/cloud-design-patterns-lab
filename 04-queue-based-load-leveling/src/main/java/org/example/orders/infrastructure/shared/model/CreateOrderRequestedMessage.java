package org.example.orders.infrastructure.shared.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateOrderRequestedMessage(
    UUID customerId, BigDecimal totalAmount, LocalDateTime requestedAt, String trackingId) {}
