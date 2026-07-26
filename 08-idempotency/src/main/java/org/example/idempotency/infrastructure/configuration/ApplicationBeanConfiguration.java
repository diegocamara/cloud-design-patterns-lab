package org.example.idempotency.infrastructure.configuration;

import org.example.idempotency.application.inventory.port.InventoryRepository;
import org.example.idempotency.application.inventory.usercase.InventoryDecreaser;
import org.example.idempotency.application.loyaltyaccount.port.LoyaltyAccountsRepository;
import org.example.idempotency.application.loyaltyaccount.usercase.LoyaltyAccountCreditor;
import org.example.idempotency.application.task.port.TasksRepository;
import org.example.idempotency.application.task.usercase.TaskCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationBeanConfiguration {
  @Bean
  public TaskCreator taskCreator(TasksRepository tasksRepository) {
    return new TaskCreator(tasksRepository);
  }

  @Bean
  public LoyaltyAccountCreditor loyaltyAccountCreditor(
      LoyaltyAccountsRepository loyaltyAccountsRepository) {
    return new LoyaltyAccountCreditor(loyaltyAccountsRepository);
  }

  @Bean
  public InventoryDecreaser inventoryDecreaser(InventoryRepository inventoryRepository) {
    return new InventoryDecreaser(inventoryRepository);
  }
}
