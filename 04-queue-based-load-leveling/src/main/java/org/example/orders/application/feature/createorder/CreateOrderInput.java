package org.example.orders.application.feature.createorder;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderInput(UUID customerId, BigDecimal totalAmount) {}
