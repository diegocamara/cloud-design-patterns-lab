package org.example.cacheaside.application.usercase;

import java.util.UUID;
import org.example.cacheaside.application.exception.ProductNotFoundException;
import org.example.cacheaside.application.port.repository.ProductsRepository;
import org.example.cacheaside.domain.model.Product;

public final class ByIdProductFinder {

  private final ProductsRepository productsRepository;

  public ByIdProductFinder(ProductsRepository productsRepository) {
    this.productsRepository = productsRepository;
  }

  public Product find(UUID id) {
    return this.productsRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
  }
}
