package org.example.orders;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class OrdersInfrastructureExtension implements BeforeAllCallback, AfterAllCallback {

  public static final String POSTGRES_SERVICE_NAME = "postgres-1";
  public static final String RABBITMQ_SERVICE_NAME = "rabbitmq-1";
  public static final int POSTGRES_SERVICE_PORT = 5432;
  public static final int RABBITMQ_AMQP_SERVICE_PORT = 5672;
  public static final int RABBITMQ_HTTP_SERVICE_PORT = 15672;
  public static final String DATABASE_USERNAME = "orders_user";
  public static final String DATABASE_PASSWORD = "orders_pass";

  public static final String RABBITMQ_USERNAME = "orders_user";
  public static final String RABBITMQ_PASSWORD = "orders_pass";
  public static final String RABBITMQ_VHOST = "orders";
  public static final String DATABASE_NAME = "orders_db";

  private final ComposeContainer composeContainer =
      new ComposeContainer(new File("env/docker-compose.yml"))
          .withExposedService(POSTGRES_SERVICE_NAME, POSTGRES_SERVICE_PORT, Wait.forListeningPort())
          .withExposedService(
              RABBITMQ_SERVICE_NAME, RABBITMQ_AMQP_SERVICE_PORT, Wait.forListeningPort())
          .withExposedService(
              RABBITMQ_SERVICE_NAME, RABBITMQ_HTTP_SERVICE_PORT, Wait.forListeningPort());

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    this.composeContainer.start();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    this.composeContainer.stop();
  }

  public String getPostgresHost() {

    return this.composeContainer.getServiceHost(POSTGRES_SERVICE_NAME, POSTGRES_SERVICE_PORT);
  }

  public Integer getPostgresPort() {

    return this.composeContainer.getServicePort(POSTGRES_SERVICE_NAME, POSTGRES_SERVICE_PORT);
  }

  public String getJdbcUrl() {
    return "jdbc:postgresql://" + getPostgresHost() + ":" + getPostgresPort() + "/" + DATABASE_NAME;
  }

  public String getRabbitMqHost() {

    return this.composeContainer.getServiceHost(RABBITMQ_SERVICE_NAME, RABBITMQ_AMQP_SERVICE_PORT);
  }

  public Integer getRabbitMqAmqpPort() {

    return this.composeContainer.getServicePort(RABBITMQ_SERVICE_NAME, RABBITMQ_AMQP_SERVICE_PORT);
  }

  public String getRabbitMqHttpHost() {

    return this.composeContainer.getServiceHost(RABBITMQ_SERVICE_NAME, RABBITMQ_HTTP_SERVICE_PORT);
  }

  public Integer getRabbitMqHttpPort() {

    return this.composeContainer.getServicePort(RABBITMQ_SERVICE_NAME, RABBITMQ_HTTP_SERVICE_PORT);
  }

  public String getRabbitMqHttpUrl() {
    return "http://" + getRabbitMqHttpHost() + ":" + getRabbitMqHttpPort();
  }

  public void stopPostgreSQL() {
    var postgreSQLContainer = getPostgreSQLContainer();
    postgreSQLContainer
        .getDockerClient()
        .stopContainerCmd(postgreSQLContainer.getContainerId())
        .withTimeout(10)
        .exec();
  }

  public void startPostgreSQL() {
    var postgreSQLContainer = getPostgreSQLContainer();
    postgreSQLContainer
        .getDockerClient()
        .startContainerCmd(postgreSQLContainer.getContainerId())
        .exec();
  }

  public void stopRabbitMQ() {
    var rabbitMQContainer = getRabbitMQContainer();
    rabbitMQContainer
        .getDockerClient()
        .stopContainerCmd(rabbitMQContainer.getContainerId())
        .withTimeout(10)
        .exec();
  }

  public void startRabbitMQ() {
    var rabbitMQContainer = getRabbitMQContainer();
    rabbitMQContainer
        .getDockerClient()
        .startContainerCmd(rabbitMQContainer.getContainerId())
        .exec();
  }

  public boolean isRabbitMQAvailable() {
    return isPortAvailable(getRabbitMqHost(), getRabbitMqAmqpPort())
        && isPortAvailable(getRabbitMqHttpHost(), getRabbitMqHttpPort());
  }

  private org.testcontainers.containers.ContainerState getPostgreSQLContainer() {
    return this.composeContainer
        .getContainerByServiceName(POSTGRES_SERVICE_NAME)
        .orElseThrow(() -> new IllegalStateException("PostgreSQL container was not found"));
  }

  private org.testcontainers.containers.ContainerState getRabbitMQContainer() {
    return this.composeContainer
        .getContainerByServiceName(RABBITMQ_SERVICE_NAME)
        .orElseThrow(() -> new IllegalStateException("RabbitMQ container was not found"));
  }

  private static boolean isPortAvailable(String host, int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), 500);
      return true;
    } catch (IOException exception) {
      return false;
    }
  }
}
