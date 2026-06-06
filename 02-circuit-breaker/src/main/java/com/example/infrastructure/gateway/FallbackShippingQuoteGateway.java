package com.example.infrastructure.gateway;

import com.example.domain.gateway.ShippingQuote;
import com.example.domain.gateway.ShippingQuoteGateway;
import com.example.domain.gateway.ShippingQuoteRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

public class FallbackShippingQuoteGateway implements ShippingQuoteGateway {

  private final ShippingQuoteGateway primaryGateway;
  private final ShippingQuoteGateway fallbackGateway;

  public FallbackShippingQuoteGateway(
      ShippingQuoteGateway primaryGateway, ShippingQuoteGateway fallbackGateway) {
    this.primaryGateway = primaryGateway;
    this.fallbackGateway = fallbackGateway;
  }

  @Override
  public ShippingQuote quote(ShippingQuoteRequest shippingQuoteRequest) {

    try {
      return this.primaryGateway.quote(shippingQuoteRequest);
    } catch (CallNotPermittedException exception) {
      return this.fallbackGateway.quote(shippingQuoteRequest);
    }
  }
}
