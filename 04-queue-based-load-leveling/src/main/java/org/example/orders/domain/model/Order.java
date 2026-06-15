package org.example.orders.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Order {

  private final UUID id;

  private final UUID customerId;

  private final OrderStatus status;

  private final BigDecimal totalAmount;

  private final LocalDateTime createdAt;

  private final LocalDateTime updatedAt;

  public Order(
      UUID id,
      UUID customerId,
      OrderStatus status,
      BigDecimal totalAmount,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.customerId = customerId;
    this.status = status;
    this.totalAmount = totalAmount;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
