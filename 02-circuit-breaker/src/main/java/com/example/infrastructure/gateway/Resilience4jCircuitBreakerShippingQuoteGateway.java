package com.example.infrastructure.gateway;

import com.example.domain.gateway.ShippingQuote;
import com.example.domain.gateway.ShippingQuoteGateway;
import com.example.domain.gateway.ShippingQuoteRequest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.function.Supplier;

public class Resilience4jCircuitBreakerShippingQuoteGateway implements ShippingQuoteGateway {

  private final ShippingQuoteGateway delegate;
  private final CircuitBreaker circuitBreaker;

  public Resilience4jCircuitBreakerShippingQuoteGateway(
      ShippingQuoteGateway delegate, CircuitBreaker circuitBreaker) {
    this.delegate = delegate;
    this.circuitBreaker = circuitBreaker;
  }

  @Override
  public ShippingQuote quote(ShippingQuoteRequest shippingQuoteRequest) {
    Supplier<ShippingQuote> supplier =
        CircuitBreaker.decorateSupplier(
            this.circuitBreaker, () -> this.delegate.quote(shippingQuoteRequest));
    return supplier.get();
  }
}
