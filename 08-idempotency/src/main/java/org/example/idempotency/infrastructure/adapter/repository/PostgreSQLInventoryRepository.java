package org.example.idempotency.infrastructure.adapter.repository;

import java.util.Optional;
import java.util.UUID;
import org.example.idempotency.application.inventory.port.InventoryRepository;
import org.example.idempotency.domain.inventory.model.Inventory;
import org.example.idempotency.infrastructure.adapter.repository.springdata.SpringDataJpaInventoryRepository;
import org.example.idempotency.infrastructure.adapter.repository.springdata.model.InventoryEntity;
import org.springframework.stereotype.Repository;

@Repository
public class PostgreSQLInventoryRepository implements InventoryRepository {

  private final SpringDataJpaInventoryRepository springDataJpaInventoryRepository;

  public PostgreSQLInventoryRepository(
      SpringDataJpaInventoryRepository springDataJpaInventoryRepository) {
    this.springDataJpaInventoryRepository = springDataJpaInventoryRepository;
  }

  @Override
  public Optional<Inventory> findByProductId(UUID productId) {
    return this.springDataJpaInventoryRepository.findById(productId).map(this::toInventory);
  }

  @Override
  public void save(Inventory inventory) {
    this.springDataJpaInventoryRepository.save(new InventoryEntity(inventory));
  }

  private Inventory toInventory(InventoryEntity inventoryEntity) {
    return Inventory.of(
        inventoryEntity.getProductId(),
        inventoryEntity.getAvailableQuantity(),
        inventoryEntity.getUpdatedAt());
  }
}
