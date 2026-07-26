package org.example.idempotency.domain.inventory.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Inventory {
  private final ProductId productId;
  private AvailableQuantity availableQuantity;
  private OffsetDateTime updateAt;

  private Inventory(
      ProductId productId, AvailableQuantity availableQuantity, OffsetDateTime updateAt) {
    this.productId = productId;
    this.availableQuantity = availableQuantity;
    this.updateAt = updateAt;
  }

  public static Inventory of(UUID productId, int availableQuantity, OffsetDateTime updateAt) {
    return new Inventory(
        new ProductId(productId), new AvailableQuantity(availableQuantity), updateAt);
  }

  public void decrease(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero");
    }

    if (this.availableQuantity.value() < quantity) {
      throw new IllegalStateException("Insufficient stock");
    }

    this.availableQuantity = new AvailableQuantity(this.availableQuantity.value() - quantity);
    this.updateAt = OffsetDateTime.now();
  }

  public ProductId getProductId() {
    return productId;
  }

  public AvailableQuantity getAvailableQuantity() {
    return availableQuantity;
  }

  public OffsetDateTime getUpdateAt() {
    return updateAt;
  }
}
