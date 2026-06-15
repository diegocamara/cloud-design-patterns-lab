package org.example.orders.infrastructure.web.publisher;

import org.example.orders.infrastructure.shared.model.CreateOrderRequestedMessage;

public interface CreateOrderRequestedMessagePublisher {

  void publish(CreateOrderRequestedMessage createOrderRequestedMessage);
}
