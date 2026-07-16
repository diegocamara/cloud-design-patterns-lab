package org.example.cacheaside;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.example.cacheaside.environment.DockerComposeEnvironmentExtension;
import org.example.cacheaside.infrastructure.web.model.request.CreateProductRequest;
import org.example.cacheaside.infrastructure.web.model.request.UpdateProductRequest;
import org.example.cacheaside.infrastructure.web.model.response.ProductResponse;
import org.example.cacheaside.postgres.PostgresClient;
import org.example.cacheaside.postgres.PostgresExtension;
import org.example.cacheaside.redis.RedisClient;
import org.example.cacheaside.redis.RedisExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith({
  DockerComposeEnvironmentExtension.class,
  PostgresExtension.class,
  RedisExtension.class
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CacheAsideIntegratedTests {

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @LocalServerPort private int port;

  @Autowired private ObjectMapper objectMapper;

  @DynamicPropertySource
  static void configureApplicationProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", DockerComposeEnvironmentExtension::postgresJdbcUrl);
    registry.add("spring.datasource.username", DockerComposeEnvironmentExtension::postgresUsername);
    registry.add("spring.datasource.password", DockerComposeEnvironmentExtension::postgresPassword);
    registry.add("spring.data.redis.host", DockerComposeEnvironmentExtension::redisHost);
    registry.add("spring.data.redis.port", DockerComposeEnvironmentExtension::redisPort);
  }

  @Test
  void shouldPopulateCacheAfterFirstProductLookup(RedisClient redis) {
    final var product = createProduct("Mechanical Keyboard", "249.90");

    assertThat(redis.exists(cacheKey(product.id()))).isFalse();

    final var productFound = findProduct(product.id());

    assertThat(productFound.name()).isEqualTo(product.name());
    assertThat(productFound.price()).isEqualByComparingTo(product.price());
    assertCachedProduct(redis, product.id(), product.name(), product.price());
  }

  @Test
  void shouldReturnCachedProductWithoutQueryingDatabaseAgain(PostgresClient postgres, RedisClient redis) {
    final var product = createProduct("Wireless Mouse", "89.90");

    findProduct(product.id());

    postgres.update("update products set price = ? where id = ?", price("119.90"), product.id());

    final var cachedProduct = findProduct(product.id());

    assertThat(cachedProduct.price()).isEqualByComparingTo("89.90");
    assertProductPriceInDatabase(postgres, product.id(), "119.90");
    assertCachedProduct(redis, product.id(), product.name(), product.price());
  }

  @Test
  void shouldEvictCachedProductWhenProductIsUpdated(RedisClient redis) {
    final var product = createProduct("USB-C Hub", "159.90");

    findProduct(product.id());
    assertCachedProduct(redis, product.id(), product.name(), product.price());

    updateProductPrice(product.id(), "199.90");

    assertThat(redis.exists(cacheKey(product.id()))).isFalse();

    final var productFound = findProduct(product.id());

    assertThat(productFound.price()).isEqualByComparingTo("199.90");
    assertCachedProduct(redis, product.id(), product.name(), price("199.90"));
  }

  @Test
  void shouldNotPopulateCacheWhenProductDoesNotExist(RedisClient redis) {
    final var productId = UUID.randomUUID();

    final var response = send(get("/products/" + productId));

    assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(redis.exists(cacheKey(productId))).isFalse();
  }

  @Test
  void shouldNotPopulateCacheWhenProductIsCreated(RedisClient redis) {
    final var product = createProduct("Laptop Stand", "179.90");

    assertThat(redis.exists(cacheKey(product.id()))).isFalse();
  }

  @Test
  void shouldFallbackToDatabaseWhenCachedProductIsCorrupted(RedisClient redis) {
    final var product = createProduct("Noise Cancelling Headphones", "699.90");

    redis.set(cacheKey(product.id()), "{invalid-json");

    final var productFound = findProduct(product.id());

    assertThat(productFound.name()).isEqualTo(product.name());
    assertThat(productFound.price()).isEqualByComparingTo(product.price());
    assertCachedProduct(redis, product.id(), product.name(), product.price());
  }

  @Test
  void shouldStoreCachedProductWithExpiration(RedisClient redis) {
    final var product = createProduct("Portable Monitor", "999.90");

    findProduct(product.id());

    assertThat(redis.ttl(cacheKey(product.id()))).isPositive();
  }

  @Test
  void shouldCacheOnlyTheRequestedProduct(RedisClient redis) {
    final var requestedProduct = createProduct("Docking Station", "499.90");
    final var untouchedProduct = createProduct("Webcam", "299.90");

    findProduct(requestedProduct.id());

    assertCachedProduct(
        redis, requestedProduct.id(), requestedProduct.name(), requestedProduct.price());
    assertThat(redis.exists(cacheKey(untouchedProduct.id()))).isFalse();

    findProduct(untouchedProduct.id());

    assertCachedProduct(redis, untouchedProduct.id(), untouchedProduct.name(), untouchedProduct.price());
  }

  @Test
  void shouldNotPopulateCacheWhenUncachedProductIsUpdated(RedisClient redis) {
    final var product = createProduct("Ergonomic Chair", "1299.90");

    updateProductPrice(product.id(), "1399.90");

    assertThat(redis.exists(cacheKey(product.id()))).isFalse();

    final var productFound = findProduct(product.id());

    assertThat(productFound.price()).isEqualByComparingTo("1399.90");
    assertCachedProduct(redis, product.id(), product.name(), price("1399.90"));
  }

  @Test
  void shouldKeepCacheEmptyAfterRepeatedLookupsForUnknownProduct(RedisClient redis) {
    final var productId = UUID.randomUUID();

    final var firstResponse = send(get("/products/" + productId));
    final var secondResponse = send(get("/products/" + productId));

    assertThat(firstResponse.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(secondResponse.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(redis.exists(cacheKey(productId))).isFalse();
  }

  @Test
  void shouldRepopulateCacheAfterManualEviction(RedisClient redis) {
    final var product = createProduct("Desk Lamp", "149.90");

    findProduct(product.id());
    assertCachedProduct(redis, product.id(), product.name(), product.price());

    redis.delete(cacheKey(product.id()));

    assertThat(redis.exists(cacheKey(product.id()))).isFalse();

    final var productFound = findProduct(product.id());

    assertThat(productFound.price()).isEqualByComparingTo(product.price());
    assertCachedProduct(redis, product.id(), product.name(), product.price());
  }

  private ProductResponse createProduct(String name, String price) {
    final var response = send(post("/products", new CreateProductRequest(name, price(price))));

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    return readResponse(response, ProductResponse.class);
  }

  private ProductResponse findProduct(UUID productId) {
    final var response = send(get("/products/" + productId));

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    return readResponse(response, ProductResponse.class);
  }

  private ProductResponse updateProductPrice(UUID productId, String price) {
    final var response = send(put("/products/" + productId, new UpdateProductRequest(price(price))));

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    return readResponse(response, ProductResponse.class);
  }

  private void assertProductPriceInDatabase(PostgresClient postgres, UUID productId, String price) {
    assertThat(
            postgres.queryForList("select price from products where id = ?", productId).stream()
                .findFirst())
        .get()
        .extracting(row -> row.get("price"))
        .satisfies(productPrice -> assertThat((BigDecimal) productPrice).isEqualByComparingTo(price));
  }

  private void assertCachedProduct(RedisClient redis, UUID productId, String name, BigDecimal price) {
    assertThat(redis.exists(cacheKey(productId))).isTrue();
    assertThat(redis.get(cacheKey(productId)))
        .contains(productId.toString())
        .contains(name)
        .contains(price.toPlainString());
  }

  private String cacheKey(UUID productId) {
    return "product:" + productId;
  }

  private BigDecimal price(String value) {
    return new BigDecimal(value);
  }

  private HttpRequest get(String path) {
    return HttpRequest.newBuilder(uri(path)).GET().build();
  }

  private HttpRequest post(String path, Object body) {
    return requestWithJsonBody(path, "POST", body);
  }

  private HttpRequest put(String path, Object body) {
    return requestWithJsonBody(path, "PUT", body);
  }

  private HttpRequest requestWithJsonBody(String path, String method, Object body) {
    return HttpRequest.newBuilder(uri(path))
        .header("Content-Type", "application/json")
        .method(method, HttpRequest.BodyPublishers.ofString(writeJson(body)))
        .build();
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  private HttpResponse<String> send(HttpRequest request) {
    try {
      return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("HTTP request was interrupted", exception);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not execute HTTP request", exception);
    }
  }

  private String writeJson(Object value) {
    return objectMapper.writeValueAsString(value);
  }

  private <T> T readResponse(HttpResponse<String> response, Class<T> type) {
    return objectMapper.readValue(response.body(), type);
  }
}
