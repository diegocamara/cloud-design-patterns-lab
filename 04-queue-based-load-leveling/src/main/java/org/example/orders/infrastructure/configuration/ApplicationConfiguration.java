package org.example.orders.infrastructure.configuration;

import org.example.orders.application.feature.createorder.OrderCreator;
import org.example.orders.application.feature.createorder.OrderCreatorImpl;
import org.example.orders.domain.repository.OrdersRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

  @Bean
  public OrderCreator orderCreator(OrdersRepository ordersRepository) {
    return new OrderCreatorImpl(ordersRepository);
  }
}
