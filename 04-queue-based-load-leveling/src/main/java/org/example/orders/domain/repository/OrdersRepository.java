package org.example.orders.domain.repository;

import org.example.orders.domain.model.Order;

public interface OrdersRepository {
  void save(Order order);
}
