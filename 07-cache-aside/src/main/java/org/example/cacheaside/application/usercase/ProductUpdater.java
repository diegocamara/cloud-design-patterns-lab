package org.example.cacheaside.application.usercase;

import java.util.Objects;
import org.example.cacheaside.application.exception.ProductNotFoundException;
import org.example.cacheaside.application.model.ProductUpdaterInput;
import org.example.cacheaside.application.port.repository.ProductsRepository;
import org.example.cacheaside.domain.model.Product;

public final class ProductUpdater {

  private final ProductsRepository productsRepository;

  public ProductUpdater(ProductsRepository productsRepository) {
    this.productsRepository = productsRepository;
  }

  public Product update(ProductUpdaterInput input) {
    Objects.requireNonNull(input);

    final var product =
        this.productsRepository
            .findById(input.id())
            .orElseThrow(() -> new ProductNotFoundException(input.id()));

    final var productUpdated =
        Product.of(product.getId().value(), product.getName().value(), input.price());

    this.productsRepository.save(productUpdated);

    return productUpdated;
  }
}
