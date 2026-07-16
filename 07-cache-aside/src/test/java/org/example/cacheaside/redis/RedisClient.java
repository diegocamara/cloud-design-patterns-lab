package org.example.cacheaside.redis;

import java.util.Objects;

public final class RedisClient implements AutoCloseable {

  private final io.lettuce.core.RedisClient client;
  private final io.lettuce.core.api.StatefulRedisConnection<String, String> connection;

  RedisClient(String host, int port) {
    this.client = io.lettuce.core.RedisClient.create("redis://" + host + ":" + port);
    this.connection = client.connect();
  }

  public boolean exists(String key) {
    Objects.requireNonNull(key, "key cannot be null");
    return connection.sync().exists(key) == 1;
  }

  public String get(String key) {
    Objects.requireNonNull(key, "key cannot be null");
    return connection.sync().get(key);
  }

  public void set(String key, String value) {
    Objects.requireNonNull(key, "key cannot be null");
    Objects.requireNonNull(value, "value cannot be null");
    connection.sync().set(key, value);
  }

  public long ttl(String key) {
    Objects.requireNonNull(key, "key cannot be null");
    return connection.sync().ttl(key);
  }

  public void delete(String key) {
    Objects.requireNonNull(key, "key cannot be null");
    connection.sync().del(key);
  }

  public void cleanApplicationKeys() {
    var keys = connection.sync().keys("product:*");
    if (!keys.isEmpty()) {
      connection.sync().del(keys.toArray(String[]::new));
    }
  }

  @Override
  public void close() {
    connection.close();
    client.shutdown();
  }
}
