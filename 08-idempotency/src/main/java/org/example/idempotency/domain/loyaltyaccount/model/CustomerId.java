package org.example.idempotency.domain.loyaltyaccount.model;

import java.util.Objects;
import java.util.UUID;

public record CustomerId(UUID value) {
  public CustomerId {
    Objects.requireNonNull(value);
  }
}
