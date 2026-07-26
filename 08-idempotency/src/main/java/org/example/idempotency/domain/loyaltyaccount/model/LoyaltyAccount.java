package org.example.idempotency.domain.loyaltyaccount.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class LoyaltyAccount {
  private final CustomerId customerId;
  private Points points;
  private OffsetDateTime updatedAt;

  private LoyaltyAccount(CustomerId customerId, Points points, OffsetDateTime updatedAt) {
    this(customerId, points);
    this.updatedAt = updatedAt;
  }

  private LoyaltyAccount(CustomerId customerId, Points points) {
    this.customerId = customerId;
    this.points = points;
  }

  //  public static LoyaltyAccount of(UUID customerId, long points) {
  //    return new LoyaltyAccount(new CustomerId(customerId), new Points(points));
  //  }

  public static LoyaltyAccount of(UUID customerId, long points, OffsetDateTime updatedAt) {
    return new LoyaltyAccount(new CustomerId(customerId), new Points(points), updatedAt);
  }

  public void credit(long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Amount must be positive");
    }
    this.points = new Points(this.points.value() + amount);
    this.updatedAt = OffsetDateTime.now();
  }

  public CustomerId getCustomerId() {
    return customerId;
  }

  public Points getPoints() {
    return points;
  }

  public void setPoints(Points points) {
    this.points = points;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
