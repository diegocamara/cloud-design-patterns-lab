package org.example.idempotency.application.inventory.usercase;

import java.util.Objects;
import org.example.idempotency.application.inventory.model.DecreaseInventoryInput;
import org.example.idempotency.application.inventory.port.InventoryRepository;

public final class InventoryDecreaser {

  private final InventoryRepository inventoryRepository;

  public InventoryDecreaser(InventoryRepository inventoryRepository) {
    this.inventoryRepository = inventoryRepository;
  }

  public void decrease(DecreaseInventoryInput input) {
    Objects.requireNonNull(input, "DecreaseInventoryInput must not be null");
    final var inventory = this.inventoryRepository.findByProductId(input.productId()).orElseThrow();
    inventory.decrease(input.quantity());
    this.inventoryRepository.save(inventory);
  }
}
