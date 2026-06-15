package org.example.orders;

import static org.example.orders.infrastructure.messaging.consumer.CreateOrdersRequestedConsumer.CONSUMER_ID;
import static org.example.orders.infrastructure.messaging.consumer.CreateOrdersRequestedConsumer.ORDERS_CREATE_REQUESTED_QUEUE_NAME;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.hikari.connection-timeout=1000",
      "spring.rabbitmq.listener.simple.concurrency=1",
      "spring.rabbitmq.listener.simple.max-concurrency=1",
      "spring.rabbitmq.listener.simple.prefetch=1",
      "spring.rabbitmq.listener.simple.default-requeue-rejected=true"
    })
public class OrdersPostgreSQLRecoveryE2ETests {

  @RegisterExtension
  static OrdersInfrastructureExtension ordersInfrastructureExtension =
      new OrdersInfrastructureExtension();

  @RegisterExtension
  static RabbitMQClientExtension rabbitMQClientExtension =
      new RabbitMQClientExtension(
          ordersInfrastructureExtension::getRabbitMqHttpUrl,
          OrdersInfrastructureExtension.RABBITMQ_USERNAME,
          OrdersInfrastructureExtension.RABBITMQ_PASSWORD);

  @RegisterExtension
  static PostgreSQLClientExtension postgreSQLClientExtension =
      new PostgreSQLClientExtension(
          ordersInfrastructureExtension::getJdbcUrl,
          OrdersInfrastructureExtension.DATABASE_USERNAME,
          OrdersInfrastructureExtension.DATABASE_PASSWORD);

  @Autowired private RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

  @LocalServerPort private int localServerPort;

  @DynamicPropertySource
  public static void dynamicPropertySource(DynamicPropertyRegistry registry) {
    registry.add("DATABASE_URL", ordersInfrastructureExtension::getJdbcUrl);
    registry.add("DATABASE_USERNAME", () -> OrdersInfrastructureExtension.DATABASE_USERNAME);
    registry.add("DATABASE_PASSWORD", () -> OrdersInfrastructureExtension.DATABASE_PASSWORD);
    registry.add("RABBITMQ_HOST", ordersInfrastructureExtension::getRabbitMqHost);
    registry.add("RABBITMQ_PORT", ordersInfrastructureExtension::getRabbitMqAmqpPort);
    registry.add("RABBITMQ_USERNAME", () -> OrdersInfrastructureExtension.RABBITMQ_USERNAME);
    registry.add("RABBITMQ_PASSWORD", () -> OrdersInfrastructureExtension.RABBITMQ_PASSWORD);
    registry.add("RABBITMQ_VHOST", () -> OrdersInfrastructureExtension.RABBITMQ_VHOST);
  }

  @Test
  @SneakyThrows
  void shouldEventuallyPersistQueuedOrdersAfterPostgreSQLRecovers() {
    final int totalRequests = 10;
    final var listenerContainer =
        this.rabbitListenerEndpointRegistry.getListenerContainer(CONSUMER_ID);
    boolean processingCompleted = false;

    assertNotNull(listenerContainer);

    listenerContainer.stop();
    await().atMost(Duration.ofSeconds(10)).until(() -> !listenerContainer.isRunning());

    try {
      sendConcurrentRequests(totalRequests);

      await()
          .atMost(Duration.ofSeconds(10))
          .untilAsserted(
              () ->
                  assertEquals(
                      totalRequests,
                      rabbitMQClientExtension.getReadyMessageCount(
                          OrdersInfrastructureExtension.RABBITMQ_VHOST,
                          ORDERS_CREATE_REQUESTED_QUEUE_NAME)));

      ordersInfrastructureExtension.stopPostgreSQL();
      await().atMost(Duration.ofSeconds(10)).until(() -> !postgreSQLClientExtension.isAvailable());

      listenerContainer.start();
      await().atMost(Duration.ofSeconds(10)).until(listenerContainer::isRunning);

      await()
          .atMost(Duration.ofSeconds(10))
          .untilAsserted(
              () ->
                  assertEquals(
                      totalRequests,
                      rabbitMQClientExtension.getTotalMessageCount(
                          OrdersInfrastructureExtension.RABBITMQ_VHOST,
                          ORDERS_CREATE_REQUESTED_QUEUE_NAME)));

      ordersInfrastructureExtension.startPostgreSQL();
      await().atMost(Duration.ofSeconds(30)).until(postgreSQLClientExtension::isAvailable);

      await()
          .atMost(Duration.ofSeconds(30))
          .untilAsserted(
              () -> {
                assertEquals(totalRequests, postgreSQLClientExtension.countRows("orders"));
                assertEquals(
                    0,
                    rabbitMQClientExtension.getTotalMessageCount(
                        OrdersInfrastructureExtension.RABBITMQ_VHOST,
                        ORDERS_CREATE_REQUESTED_QUEUE_NAME));
              });

      processingCompleted = true;
    } finally {
      if (!postgreSQLClientExtension.isAvailable()) {
        ordersInfrastructureExtension.startPostgreSQL();
        await().atMost(Duration.ofSeconds(30)).until(postgreSQLClientExtension::isAvailable);
      }

      if (!processingCompleted) {
        listenerContainer.stop();
        await().atMost(Duration.ofSeconds(10)).until(() -> !listenerContainer.isRunning());
        rabbitMQClientExtension.purgeAllQueues();
        postgreSQLClientExtension.truncateAllTables();
      }

      if (!listenerContainer.isRunning()) {
        listenerContainer.start();
        await().atMost(Duration.ofSeconds(10)).until(listenerContainer::isRunning);
      }
    }
  }

  @SneakyThrows
  private void sendConcurrentRequests(int totalRequests) {
    final var httpClient = HttpClient.newHttpClient();

    try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      final var futures =
          IntStream.range(0, totalRequests)
              .mapToObj(index -> executor.submit(() -> sendRequest(httpClient)))
              .toList();

      for (Future<HttpResponse<String>> future : futures) {
        assertEquals(HttpStatus.ACCEPTED.value(), future.get().statusCode());
      }
    }
  }

  @SneakyThrows
  private HttpResponse<String> sendRequest(HttpClient httpClient) {
    final var body =
        """
        {
          "customerId": "11111111-1111-1111-1111-111111111111",
          "totalAmount": 100.00
        }
        """;

    final var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + this.localServerPort + "/orders"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
