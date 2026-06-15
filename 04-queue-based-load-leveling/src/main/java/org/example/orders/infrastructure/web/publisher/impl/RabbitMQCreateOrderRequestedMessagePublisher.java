package org.example.orders.infrastructure.web.publisher.impl;

import org.example.orders.infrastructure.shared.model.CreateOrderRequestedMessage;
import org.example.orders.infrastructure.web.publisher.CreateOrderRequestedMessagePublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQCreateOrderRequestedMessagePublisher
    implements CreateOrderRequestedMessagePublisher {

  public static final String ORDERS_EXCHANGE_NAME = "orders.exchange";
  public static final String ORDERS_CREATE_REQUESTED_ROUTING_KEY = "orders.create.requested";

  private final RabbitTemplate rabbitTemplate;

  public RabbitMQCreateOrderRequestedMessagePublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  public void publish(CreateOrderRequestedMessage createOrderRequestedMessage) {
    this.rabbitTemplate.convertAndSend(
        ORDERS_EXCHANGE_NAME, ORDERS_CREATE_REQUESTED_ROUTING_KEY, createOrderRequestedMessage);
  }
}
