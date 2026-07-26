package org.example.idempotency.infrastructure.adapter.repository.springdata.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.example.idempotency.domain.loyaltyaccount.model.LoyaltyAccount;

@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccountEntity {

  @Id
  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(nullable = false)
  private long points;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public LoyaltyAccountEntity() {}

  public LoyaltyAccountEntity(LoyaltyAccount loyaltyAccount) {
    this.customerId = loyaltyAccount.getCustomerId().value();
    this.points = loyaltyAccount.getPoints().value();
    this.updatedAt = loyaltyAccount.getUpdatedAt();
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public void setCustomerId(UUID customerId) {
    this.customerId = customerId;
  }

  public long getPoints() {
    return points;
  }

  public void setPoints(long points) {
    this.points = points;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    LoyaltyAccountEntity that = (LoyaltyAccountEntity) o;
    return points == that.points
        && Objects.equals(customerId, that.customerId)
        && Objects.equals(updatedAt, that.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customerId, points, updatedAt);
  }
}
