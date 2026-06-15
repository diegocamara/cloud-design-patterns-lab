package org.example.orders.infrastructure.messaging.consumer;

import static org.example.orders.infrastructure.messaging.consumer.CreateOrderRequestedMessageToCreateOrderInputConverter.convert;

import org.example.orders.application.feature.createorder.OrderCreator;
import org.example.orders.infrastructure.shared.model.CreateOrderRequestedMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CreateOrdersRequestedConsumer {

  public static final String ORDERS_CREATE_REQUESTED_QUEUE_NAME = "orders.create.requested.queue";
  public static final String CONSUMER_ID = "create-orders-consumer";

  private final OrderCreator orderCreator;

  public CreateOrdersRequestedConsumer(OrderCreator orderCreator) {
    this.orderCreator = orderCreator;
  }

  @RabbitListener(id = CONSUMER_ID, queues = ORDERS_CREATE_REQUESTED_QUEUE_NAME)
  public void consume(CreateOrderRequestedMessage createOrderRequestedMessage) {

    this.orderCreator.create(convert(createOrderRequestedMessage));
  }
}
