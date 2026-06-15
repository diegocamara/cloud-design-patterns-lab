package org.example.orders.application.feature.createorder;

import java.time.LocalDateTime;
import java.util.UUID;
import org.example.orders.domain.model.Order;
import org.example.orders.domain.model.OrderStatus;
import org.example.orders.domain.repository.OrdersRepository;

public class OrderCreatorImpl implements OrderCreator {

  private final OrdersRepository ordersRepository;

  public OrderCreatorImpl(OrdersRepository ordersRepository) {
    this.ordersRepository = ordersRepository;
  }

  @Override
  public Order create(CreateOrderInput input) {

    final var now = LocalDateTime.now();

    Order order =
        new Order(
            UUID.randomUUID(),
            input.customerId(),
            OrderStatus.PROCESSING,
            input.totalAmount(),
            now,
            now);

    this.ordersRepository.save(order);

    return order;
  }
}
