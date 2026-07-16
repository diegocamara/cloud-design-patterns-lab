package org.example.cacheaside.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Product {
  private final Id id;
  private final Name name;
  private final Price price;

  private Product(Id id, Name name, Price price) {
    this.id = id;
    this.name = name;
    this.price = price;
  }

  public static Product of(UUID id, String name, BigDecimal price) {
    return new Product(new Id(id), new Name(name), new Price(price));
  }

  public Id getId() {
    return id;
  }

  public Name getName() {
    return name;
  }

  public Price getPrice() {
    return price;
  }
}
