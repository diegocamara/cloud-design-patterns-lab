package org.example.orders;

import com.rabbitmq.http.client.Client;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class RabbitMQClientExtension implements AfterEachCallback {

  private static final String MANAGEMENT_API_PATH = "/api/";

  private final Supplier<String> managementUrlSupplier;
  private final String username;
  private final String password;

  private Client client;

  public RabbitMQClientExtension(String managementUrl, String username, String password) {
    this(() -> managementUrl, username, password);
  }

  public RabbitMQClientExtension(
      Supplier<String> managementUrlSupplier, String username, String password) {
    this.managementUrlSupplier =
        Objects.requireNonNull(managementUrlSupplier, "managementUrlSupplier must not be null");
    this.username = Objects.requireNonNull(username, "username must not be null");
    this.password = Objects.requireNonNull(password, "password must not be null");
  }

  @Override
  public void afterEach(ExtensionContext context) {
    purgeAllQueues();
  }

  public void purgeAllQueues() {
    Client rabbitMQClient = getClient();

    rabbitMQClient
        .getVhosts()
        .forEach(
            vhost ->
                rabbitMQClient
                    .getQueues(vhost.getName())
                    .forEach(queue -> rabbitMQClient.purgeQueue(vhost.getName(), queue.getName())));
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

  private Client getClient() {
    if (this.client == null) {
      this.client = createClient();
    }

    return this.client;
  }

  private Client createClient() {
    String managementApiUrl = normalizeManagementApiUrl(this.managementUrlSupplier.get());

    try {
      return new Client(new URI(managementApiUrl).toURL(), this.username, this.password);
    } catch (MalformedURLException | URISyntaxException exception) {
      throw new IllegalArgumentException(
          "Invalid RabbitMQ Management API URL: " + managementApiUrl, exception);
    }
  }

  private static String normalizeManagementApiUrl(String managementUrl) {
    Objects.requireNonNull(managementUrl, "managementUrl must not be null");

    String normalizedUrl = managementUrl.endsWith("/") ? managementUrl : managementUrl + "/";
    if (!normalizedUrl.endsWith(MANAGEMENT_API_PATH)) {
      normalizedUrl += "api/";
    }

    return normalizedUrl;
  }
}
