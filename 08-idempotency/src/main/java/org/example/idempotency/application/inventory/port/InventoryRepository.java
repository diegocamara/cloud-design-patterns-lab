package org.example.idempotency.application.inventory.port;

import java.util.Optional;
import java.util.UUID;
import org.example.idempotency.domain.inventory.model.Inventory;

public interface InventoryRepository {
  Optional<Inventory> findByProductId(UUID productId);

  void save(Inventory inventory);
}
