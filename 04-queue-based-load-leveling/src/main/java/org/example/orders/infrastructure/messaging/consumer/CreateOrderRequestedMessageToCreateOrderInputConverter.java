package org.example.orders.infrastructure.messaging.consumer;

import org.example.orders.application.feature.createorder.CreateOrderInput;
import org.example.orders.infrastructure.shared.model.CreateOrderRequestedMessage;

public class CreateOrderRequestedMessageToCreateOrderInputConverter {

  private CreateOrderRequestedMessageToCreateOrderInputConverter() {}

  public static CreateOrderInput convert(CreateOrderRequestedMessage createOrderRequestedMessage) {
    return new CreateOrderInput(
        createOrderRequestedMessage.customerId(), createOrderRequestedMessage.totalAmount());
  }
}
