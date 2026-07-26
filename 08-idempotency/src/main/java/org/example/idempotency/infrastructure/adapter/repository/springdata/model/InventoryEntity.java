package org.example.idempotency.infrastructure.adapter.repository.springdata.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.example.idempotency.domain.inventory.model.Inventory;

@Entity
@Table(name = "inventories")
public class InventoryEntity {

  @Id
  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Column(name = "available_quantity", nullable = false)
  private int availableQuantity;

  @Version
  @Column(nullable = false)
  private long version;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected InventoryEntity() {}

  public InventoryEntity(Inventory inventory) {
    this.productId = inventory.getProductId().value();
    this.availableQuantity = inventory.getAvailableQuantity().value();
    this.updatedAt = inventory.getUpdateAt();
  }

  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID productId) {
    this.productId = productId;
  }

  public int getAvailableQuantity() {
    return availableQuantity;
  }

  public void setAvailableQuantity(int availableQuantity) {
    this.availableQuantity = availableQuantity;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
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
    InventoryEntity that = (InventoryEntity) o;
    return availableQuantity == that.availableQuantity
        && version == that.version
        && Objects.equals(productId, that.productId)
        && Objects.equals(updatedAt, that.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productId, availableQuantity, version, updatedAt);
  }
}
