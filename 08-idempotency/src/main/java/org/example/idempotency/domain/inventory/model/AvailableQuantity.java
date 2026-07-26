package org.example.idempotency.domain.inventory.model;

public record AvailableQuantity(int value) {
  public AvailableQuantity {
    if (value < 0) {
      throw new IllegalArgumentException("Quantity value cannot be negative");
    }
  }
}
