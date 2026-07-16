package org.example.cacheaside.infrastructure.cache.impl;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.example.cacheaside.infrastructure.adapters.repository.springdata.model.ProductEntity;
import org.example.cacheaside.infrastructure.cache.ProductEntityCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class StringRedisTemplateProductEntityCache implements ProductEntityCache {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(StringRedisTemplateProductEntityCache.class);

  private static final Duration TTL = Duration.ofMinutes(5);
  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public StringRedisTemplateProductEntityCache(
      StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public Optional<ProductEntity> get(UUID productId) {
    try {
      final var json = this.stringRedisTemplate.opsForValue().get(key(productId));
      if (json == null) {
        return Optional.empty();
      }
      return Optional.of(this.objectMapper.readValue(json, ProductEntity.class));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public void put(ProductEntity productEntity) {
    try {
      final var json = this.objectMapper.writeValueAsString(productEntity);
      this.stringRedisTemplate.opsForValue().set(key(productEntity.getId()), json, TTL);
    } catch (Exception exception) {
      LOGGER.error(exception.getMessage(), exception);
    }
  }

  @Override
  public void evict(UUID productId) {
    this.stringRedisTemplate.delete(key(productId));
  }

  private String key(UUID productId) {
    return "product:" + productId;
  }
}
