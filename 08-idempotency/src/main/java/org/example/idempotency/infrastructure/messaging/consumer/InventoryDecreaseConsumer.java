package org.example.idempotency.infrastructure.messaging.consumer;

import org.example.idempotency.application.inventory.model.DecreaseInventoryInput;
import org.example.idempotency.application.inventory.usercase.InventoryDecreaser;
import org.example.idempotency.infrastructure.messaging.model.InventoryDecreaseMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryDecreaseConsumer {

  private final InventoryDecreaser inventoryDecreaser;

  public InventoryDecreaseConsumer(InventoryDecreaser inventoryDecreaser) {
    this.inventoryDecreaser = inventoryDecreaser;
  }

  @RabbitListener(queues = "inventory.decrease")
  public void consumer(InventoryDecreaseMessage message) {
    this.inventoryDecreaser.decrease(
        new DecreaseInventoryInput(message.productId(), message.quantity()));
  }
}
