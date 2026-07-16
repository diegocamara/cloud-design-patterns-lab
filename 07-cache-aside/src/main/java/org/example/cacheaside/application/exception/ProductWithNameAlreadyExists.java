package org.example.cacheaside.application.exception;

public class ProductWithNameAlreadyExists extends RuntimeException {
  public ProductWithNameAlreadyExists(String productName) {
    super("Product with name " + productName + " already exists");
  }
}
