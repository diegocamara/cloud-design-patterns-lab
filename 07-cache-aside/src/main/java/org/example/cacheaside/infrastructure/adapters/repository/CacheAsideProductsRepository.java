package org.example.cacheaside.infrastructure.adapters.repository;

import java.util.Optional;
import java.util.UUID;
import org.example.cacheaside.application.port.repository.ProductsRepository;
import org.example.cacheaside.domain.model.Product;
import org.example.cacheaside.infrastructure.adapters.repository.springdata.model.ProductEntity;
import org.example.cacheaside.infrastructure.adapters.repository.springdata.model.SpringDataJpaProductsRepository;
import org.example.cacheaside.infrastructure.cache.ProductEntityCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class CacheAsideProductsRepository implements ProductsRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheAsideProductsRepository.class);

  private final SpringDataJpaProductsRepository springDataJpaProductsRepository;
  private final ProductEntityCache cache;

  public CacheAsideProductsRepository(
      SpringDataJpaProductsRepository springDataJpaProductsRepository,
      ProductEntityCache cache) {
    this.springDataJpaProductsRepository = springDataJpaProductsRepository;
    this.cache = cache;
  }

  @Override
  public boolean existsByNameIgnoreCase(String name) {
    return this.springDataJpaProductsRepository.existsByNameIgnoreCase(name);
  }

  @Override
  public void save(Product product) {
    if (this.springDataJpaProductsRepository.existsById(product.getId().value())) {
      this.cache.evict(product.getId().value());
    }
    this.springDataJpaProductsRepository.save(new ProductEntity(product));
  }

  @Override
  public Optional<Product> findById(UUID id) {

    return this.cache
        .get(id)
        .map(
            productEntity -> {
              LOGGER.info("Cache hit for product: {}", productEntity.getId());
              return productEntity;
            })
        .or(
            () -> {
              LOGGER.info("Cache miss for product: {}", id);

              final var productEntityOptional = this.springDataJpaProductsRepository.findById(id);

              productEntityOptional.ifPresent(this.cache::put);

              return productEntityOptional;
            })
        .map(this::toProduct);
  }

  private Product toProduct(ProductEntity productEntity) {
    return Product.of(productEntity.getId(), productEntity.getName(), productEntity.getPrice());
  }
}
