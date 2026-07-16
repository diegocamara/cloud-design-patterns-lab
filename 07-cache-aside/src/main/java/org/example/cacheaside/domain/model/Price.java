package org.example.cacheaside.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record Price(BigDecimal value) {
  public Price {
    Objects.requireNonNull(value);
    validPriceValue(value);
  }

  private void validPriceValue(BigDecimal value) {
    if (value.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Price value can't be negative");
    }
  }
}
