package org.example.cacheaside.application.usercase;

import java.util.Objects;
import java.util.UUID;
import org.example.cacheaside.application.exception.ProductWithNameAlreadyExists;
import org.example.cacheaside.application.model.ProductCreatorInput;
import org.example.cacheaside.application.port.repository.ProductsRepository;
import org.example.cacheaside.domain.model.Product;

public final class ProductCreator {

  private final ProductsRepository productsRepository;

  public ProductCreator(ProductsRepository productsRepository) {
    this.productsRepository = productsRepository;
  }

  public Product create(ProductCreatorInput input) {
    Objects.requireNonNull(input);

    final var product = Product.of(UUID.randomUUID(), input.name(), input.price());

    checkIfNameAlreadyExists(product);

    this.productsRepository.save(product);

    return product;
  }

  private void checkIfNameAlreadyExists(Product product) {
    if (this.productsRepository.existsByNameIgnoreCase(product.getName().value())) {
      throw new ProductWithNameAlreadyExists(product.getName().value());
    }
  }
}
