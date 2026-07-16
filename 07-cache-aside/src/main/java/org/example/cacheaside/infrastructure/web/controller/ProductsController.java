package org.example.cacheaside.infrastructure.web.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.example.cacheaside.application.model.ProductCreatorInput;
import org.example.cacheaside.application.model.ProductUpdaterInput;
import org.example.cacheaside.application.usercase.ByIdProductFinder;
import org.example.cacheaside.application.usercase.ProductCreator;
import org.example.cacheaside.application.usercase.ProductUpdater;
import org.example.cacheaside.domain.model.Product;
import org.example.cacheaside.infrastructure.web.model.request.CreateProductRequest;
import org.example.cacheaside.infrastructure.web.model.request.UpdateProductRequest;
import org.example.cacheaside.infrastructure.web.model.response.ProductResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductsController {

  private final ProductCreator productCreator;
  private final ProductUpdater productUpdater;
  private final ByIdProductFinder byIdProductFinder;

  public ProductsController(
      ProductCreator productCreator,
      ProductUpdater productUpdater,
      ByIdProductFinder byIdProductFinder) {
    this.productCreator = productCreator;
    this.productUpdater = productUpdater;
    this.byIdProductFinder = byIdProductFinder;
  }

  @PostMapping
  public ResponseEntity<ProductResponse> create(
      @Valid @NotNull @RequestBody CreateProductRequest createProductRequest) {
    final var product =
        this.productCreator.create(
            new ProductCreatorInput(createProductRequest.name(), createProductRequest.price()));
    return ResponseEntity.ok(toProductResponse(product));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ProductResponse> findById(@PathVariable @NotNull UUID id) {
    final var product = this.byIdProductFinder.find(id);
    return ResponseEntity.ok(toProductResponse(product));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ProductResponse> update(
      @PathVariable @NotNull UUID id,
      @Valid @NotNull @RequestBody UpdateProductRequest updateProductRequest) {
    final var product =
        this.productUpdater.update(new ProductUpdaterInput(id, updateProductRequest.price()));
    return ResponseEntity.ok(toProductResponse(product));
  }

  private ProductResponse toProductResponse(Product product) {
    return new ProductResponse(
        product.getId().value(), product.getName().value(), product.getPrice().value());
  }
}
