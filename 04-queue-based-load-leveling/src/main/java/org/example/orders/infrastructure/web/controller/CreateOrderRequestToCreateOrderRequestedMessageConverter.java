package org.example.orders.infrastructure.web.controller;

import java.time.LocalDateTime;
import org.example.orders.infrastructure.shared.model.CreateOrderRequestedMessage;
import org.example.orders.infrastructure.web.model.CreateOrderRequest;

public class CreateOrderRequestToCreateOrderRequestedMessageConverter {

  private CreateOrderRequestToCreateOrderRequestedMessageConverter() {}

  public static CreateOrderRequestedMessage convert(
      CreateOrderRequest createOrderRequest, String trackingId) {
    final var now = LocalDateTime.now();
    return new CreateOrderRequestedMessage(
        createOrderRequest.customerId(), createOrderRequest.totalAmount(), now, trackingId);
  }
}
