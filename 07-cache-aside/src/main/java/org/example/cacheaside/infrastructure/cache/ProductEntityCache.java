package org.example.cacheaside.infrastructure.cache;

import java.util.Optional;
import java.util.UUID;
import org.example.cacheaside.infrastructure.adapters.repository.springdata.model.ProductEntity;

public interface ProductEntityCache {

  Optional<ProductEntity> get(UUID productId);

  void put(ProductEntity productEntity);

  void evict(UUID productId);
}
