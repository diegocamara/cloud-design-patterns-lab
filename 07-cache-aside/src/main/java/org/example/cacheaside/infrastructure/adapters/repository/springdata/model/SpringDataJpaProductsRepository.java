package org.example.cacheaside.infrastructure.adapters.repository.springdata.model;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaProductsRepository extends JpaRepository<ProductEntity, UUID> {
  boolean existsByNameIgnoreCase(String name);
}
