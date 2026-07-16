package org.example.cacheaside.infrastructure.adapters.repository.springdata.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.example.cacheaside.domain.model.Product;

@Entity
@Table(name = "products")
public class ProductEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal price;

  protected ProductEntity() {}

  public ProductEntity(Product product) {
    this.id = product.getId().value();
    this.name = product.getName().value();
    this.price = product.getPrice().value();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }
}
