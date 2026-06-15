package org.example.orders.infrastructure.repository;

import org.example.orders.domain.model.Order;
import org.example.orders.domain.repository.OrdersRepository;
import org.example.orders.infrastructure.repository.springdata.entity.OrderEntity;
import org.example.orders.infrastructure.repository.springdata.repository.JpaOrdersRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresOrdersRepository implements OrdersRepository {

  private final JpaOrdersRepository jpaOrdersRepository;

  public PostgresOrdersRepository(JpaOrdersRepository jpaOrdersRepository) {
    this.jpaOrdersRepository = jpaOrdersRepository;
  }

  @Override
  public void save(Order order) {

    this.jpaOrdersRepository.save(convertToOrderEntity(order));
  }

  private OrderEntity convertToOrderEntity(Order order) {
    return new OrderEntity(
        order.getId(),
        order.getCustomerId(),
        order.getStatus(),
        order.getTotalAmount(),
        order.getCreatedAt(),
        order.getUpdatedAt());
  }
}
