package org.example.orders.application.feature.createorder;

import org.example.orders.domain.model.Order;

public interface OrderCreator {
  Order create(CreateOrderInput input);
}
