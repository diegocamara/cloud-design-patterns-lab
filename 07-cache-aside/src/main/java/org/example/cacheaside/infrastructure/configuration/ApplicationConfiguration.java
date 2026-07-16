package org.example.cacheaside.infrastructure.configuration;

import org.example.cacheaside.application.port.repository.ProductsRepository;
import org.example.cacheaside.application.usercase.ByIdProductFinder;
import org.example.cacheaside.application.usercase.ProductCreator;
import org.example.cacheaside.application.usercase.ProductUpdater;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

  @Bean
  public ProductCreator productCreator(ProductsRepository productsRepository) {
    return new ProductCreator(productsRepository);
  }

  @Bean
  public ProductUpdater productUpdater(ProductsRepository productsRepository) {
    return new ProductUpdater(productsRepository);
  }

  @Bean
  public ByIdProductFinder byIdProductFinder(ProductsRepository productsRepository) {
    return new ByIdProductFinder(productsRepository);
  }
}
