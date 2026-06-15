package org.example.orders.infrastructure.web.controller;

import static org.example.orders.infrastructure.web.controller.CreateOrderRequestToCreateOrderRequestedMessageConverter.convert;

import jakarta.validation.Valid;
import java.util.UUID;
import org.example.orders.infrastructure.web.model.CreateOrderRequest;
import org.example.orders.infrastructure.web.model.CreateOrderResponse;
import org.example.orders.infrastructure.web.publisher.CreateOrderRequestedMessagePublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrdersController {

  private final CreateOrderRequestedMessagePublisher createOrderRequestedMessagePublisher;

  public OrdersController(
      CreateOrderRequestedMessagePublisher createOrderRequestedMessagePublisher) {
    this.createOrderRequestedMessagePublisher = createOrderRequestedMessagePublisher;
  }

  @PostMapping
  public ResponseEntity<CreateOrderResponse> create(
      @Valid @RequestBody CreateOrderRequest createOrderRequest) {

    final var trackingId = UUID.randomUUID().toString();

    this.createOrderRequestedMessagePublisher.publish(convert(createOrderRequest, trackingId));

    return ResponseEntity.accepted().body(new CreateOrderResponse(trackingId));
  }
}
