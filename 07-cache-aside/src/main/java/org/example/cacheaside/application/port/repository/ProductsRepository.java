package org.example.cacheaside.application.port.repository;

import java.util.Optional;
import java.util.UUID;
import org.example.cacheaside.domain.model.Product;

public interface ProductsRepository {
  boolean existsByNameIgnoreCase(String name);

  void save(Product product);

  Optional<Product> findById(UUID id);
}
