package org.example.idempotency.domain.loyaltyaccount.model;

public record Points(long value) {
  public Points {
    if (value < 0) {
      throw new IllegalArgumentException("Points cannot be negative");
    }
  }
}
