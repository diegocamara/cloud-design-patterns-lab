package org.example.idempotency.environment;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.BindingInfo;
import com.rabbitmq.http.client.domain.ExchangeInfo;
import com.rabbitmq.http.client.domain.QueueInfo;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

public final class RabbitMQClientExtension
    implements BeforeAllCallback, AfterEachCallback, AfterAllCallback, ParameterResolver {

  private static final String MANAGEMENT_API_PATH = "/api/";
  private static final String USERNAME = "idempotency_user";
  private static final String PASSWORD = "idempotency_pass";
  private static final String VIRTUAL_HOST = "idempotency";
  private static final String INVENTORY_DECREASE_QUEUE = "inventory.decrease";

  private final RabbitMQContainer container =
      new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.3.1-management-alpine"))
          .withAdminUser(USERNAME)
          .withAdminPassword(PASSWORD)
          .withEnv("RABBITMQ_DEFAULT_VHOST", VIRTUAL_HOST);

  private Client client;

  @Override
  public void beforeAll(ExtensionContext context) {
    this.container.start();
    declareApplicationQueues();
  }

  @Override
  public void afterEach(ExtensionContext context) {
    purgeAllQueues();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    this.container.stop();
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType().equals(Client.class);
  }

  @Override
  public Client resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return getClient();
  }

  public void purgeAllQueues() {
    var rabbitMQClient = getClient();

    rabbitMQClient
        .getVhosts()
        .forEach(
            vhost ->
                rabbitMQClient
                    .getQueues(vhost.getName())
                    .forEach(queue -> rabbitMQClient.purgeQueue(vhost.getName(), queue.getName())));
  }

  public void declareQueue(String queueName) {
    declareQueue(virtualHost(), queueName, true, false, false, Map.of());
  }

  public void declareQueue(
      String vhost,
      String queueName,
      boolean durable,
      boolean exclusive,
      boolean autoDelete,
      Map<String, Object> arguments) {
    requireText(vhost, "vhost");
    requireText(queueName, "queueName");
    Objects.requireNonNull(arguments, "arguments must not be null");

    getClient()
        .declareQueue(
            vhost, queueName, new QueueInfo(durable, exclusive, autoDelete, arguments));
  }

  public void deleteQueue(String queueName) {
    deleteQueue(virtualHost(), queueName);
  }

  public void deleteQueue(String vhost, String queueName) {
    requireText(vhost, "vhost");
    requireText(queueName, "queueName");
    getClient().deleteQueue(vhost, queueName);
  }

  public void purgeQueue(String queueName) {
    purgeQueue(virtualHost(), queueName);
  }

  public void purgeQueue(String vhost, String queueName) {
    requireText(vhost, "vhost");
    requireText(queueName, "queueName");
    getClient().purgeQueue(vhost, queueName);
  }

  public void declareExchange(String exchangeName, String type) {
    declareExchange(virtualHost(), exchangeName, type, true, false, false, Map.of());
  }

  public void declareExchange(
      String vhost,
      String exchangeName,
      String type,
      boolean durable,
      boolean autoDelete,
      boolean internal,
      Map<String, Object> arguments) {
    requireText(vhost, "vhost");
    requireText(exchangeName, "exchangeName");
    requireText(type, "type");
    Objects.requireNonNull(arguments, "arguments must not be null");

    getClient()
        .declareExchange(
            vhost,
            exchangeName,
            new ExchangeInfo(type, durable, autoDelete, internal, arguments));
  }

  public void deleteExchange(String exchangeName) {
    deleteExchange(virtualHost(), exchangeName);
  }

  public void deleteExchange(String vhost, String exchangeName) {
    requireText(vhost, "vhost");
    requireText(exchangeName, "exchangeName");
    getClient().deleteExchange(vhost, exchangeName);
  }

  public void bindQueue(String exchangeName, String queueName, String routingKey) {
    bindQueue(virtualHost(), exchangeName, queueName, routingKey, Map.of());
  }

  public void bindQueue(
      String vhost,
      String exchangeName,
      String queueName,
      String routingKey,
      Map<String, Object> arguments) {
    requireText(vhost, "vhost");
    requireText(exchangeName, "exchangeName");
    requireText(queueName, "queueName");
    Objects.requireNonNull(routingKey, "routingKey must not be null");
    Objects.requireNonNull(arguments, "arguments must not be null");

    getClient().bindQueue(vhost, queueName, exchangeName, routingKey, arguments);
  }

  public void unbindQueue(String exchangeName, String queueName, String routingKey) {
    unbindQueue(virtualHost(), exchangeName, queueName, routingKey);
  }

  public void unbindQueue(
      String vhost, String exchangeName, String queueName, String routingKey) {
    requireText(vhost, "vhost");
    requireText(exchangeName, "exchangeName");
    requireText(queueName, "queueName");
    Objects.requireNonNull(routingKey, "routingKey must not be null");

    getClient().unbindQueue(vhost, queueName, exchangeName, routingKey);
  }

  public void bindExchange(
      String sourceExchange, String destinationExchange, String routingKey) {
    bindExchange(virtualHost(), sourceExchange, destinationExchange, routingKey, Map.of());
  }

  public void bindExchange(
      String vhost,
      String sourceExchange,
      String destinationExchange,
      String routingKey,
      Map<String, Object> arguments) {
    requireText(vhost, "vhost");
    requireText(sourceExchange, "sourceExchange");
    requireText(destinationExchange, "destinationExchange");
    Objects.requireNonNull(routingKey, "routingKey must not be null");
    Objects.requireNonNull(arguments, "arguments must not be null");

    getClient()
        .bindExchange(vhost, destinationExchange, sourceExchange, routingKey, arguments);
  }

  public void unbindExchange(
      String sourceExchange, String destinationExchange, String routingKey) {
    unbindExchange(virtualHost(), sourceExchange, destinationExchange, routingKey);
  }

  public void unbindExchange(
      String vhost, String sourceExchange, String destinationExchange, String routingKey) {
    requireText(vhost, "vhost");
    requireText(sourceExchange, "sourceExchange");
    requireText(destinationExchange, "destinationExchange");
    Objects.requireNonNull(routingKey, "routingKey must not be null");

    getClient().unbindExchange(vhost, destinationExchange, sourceExchange, routingKey);
  }

  public List<BindingInfo> getQueueBindings(String queueName) {
    return getQueueBindings(virtualHost(), queueName);
  }

  public List<BindingInfo> getQueueBindings(String vhost, String queueName) {
    requireText(vhost, "vhost");
    requireText(queueName, "queueName");
    return getClient().getQueueBindings(vhost, queueName);
  }

  public List<BindingInfo> getExchangeBindings(String exchangeName) {
    return getExchangeBindings(virtualHost(), exchangeName);
  }

  public List<BindingInfo> getExchangeBindings(String vhost, String exchangeName) {
    requireText(vhost, "vhost");
    requireText(exchangeName, "exchangeName");
    return getClient().getBindingsBySource(vhost, exchangeName);
  }

  public void createVhost(String vhost) {
    requireText(vhost, "vhost");
    getClient().createVhost(vhost);
  }

  public void deleteVhost(String vhost) {
    requireText(vhost, "vhost");
    getClient().deleteVhost(vhost);
  }

  public long getReadyMessageCount(String vhost, String queueName) {
    Objects.requireNonNull(vhost, "vhost must not be null");
    Objects.requireNonNull(queueName, "queueName must not be null");

    return getClient().getQueue(vhost, queueName).getMessagesReady();
  }

  public long getUnacknowledgedMessageCount(String vhost, String queueName) {
    Objects.requireNonNull(vhost, "vhost must not be null");
    Objects.requireNonNull(queueName, "queueName must not be null");

    return getClient().getQueue(vhost, queueName).getMessagesUnacknowledged();
  }

  public long getTotalMessageCount(String vhost, String queueName) {
    Objects.requireNonNull(vhost, "vhost must not be null");
    Objects.requireNonNull(queueName, "queueName must not be null");

    return getClient().getQueue(vhost, queueName).getTotalMessages();
  }

  public boolean isQueueAvailable(String vhost, String queueName) {
    Objects.requireNonNull(vhost, "vhost must not be null");
    Objects.requireNonNull(queueName, "queueName must not be null");

    try {
      return getClient().getQueue(vhost, queueName) != null;
    } catch (RuntimeException exception) {
      return false;
    }
  }

  public boolean isExchangeAvailable(String vhost, String exchangeName) {
    requireText(vhost, "vhost");
    requireText(exchangeName, "exchangeName");

    try {
      return getClient().getExchange(vhost, exchangeName) != null;
    } catch (RuntimeException exception) {
      return false;
    }
  }

  public String host() {
    return this.container.getHost();
  }

  public int amqpPort() {
    return this.container.getAmqpPort();
  }

  public String username() {
    return USERNAME;
  }

  public String password() {
    return PASSWORD;
  }

  public String virtualHost() {
    return VIRTUAL_HOST;
  }

  private Client getClient() {
    if (this.client == null) {
      this.client = createClient();
    }

    return this.client;
  }

  private Client createClient() {
    var managementApiUrl = normalizeManagementApiUrl(this.container.getHttpUrl());

    try {
      return new Client(new URI(managementApiUrl).toURL(), username(), password());
    } catch (MalformedURLException | URISyntaxException exception) {
      throw new IllegalArgumentException(
          "Invalid RabbitMQ Management API URL: " + managementApiUrl, exception);
    }
  }

  private void declareApplicationQueues() {
    var connectionFactory = new ConnectionFactory();
    connectionFactory.setHost(host());
    connectionFactory.setPort(amqpPort());
    connectionFactory.setUsername(username());
    connectionFactory.setPassword(password());
    connectionFactory.setVirtualHost(virtualHost());

    try (var connection = connectionFactory.newConnection();
        var channel = connection.createChannel()) {
      channel.queueDeclare(INVENTORY_DECREASE_QUEUE, true, false, false, null);
    } catch (IOException | TimeoutException exception) {
      throw new IllegalStateException("Could not declare RabbitMQ application queues", exception);
    }
  }

  private static String normalizeManagementApiUrl(String managementUrl) {
    Objects.requireNonNull(managementUrl, "managementUrl must not be null");

    var normalizedUrl = managementUrl.endsWith("/") ? managementUrl : managementUrl + "/";
    if (!normalizedUrl.endsWith(MANAGEMENT_API_PATH)) {
      normalizedUrl += "api/";
    }

    return normalizedUrl;
  }

  private static void requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be null or blank");
    }
  }
}
