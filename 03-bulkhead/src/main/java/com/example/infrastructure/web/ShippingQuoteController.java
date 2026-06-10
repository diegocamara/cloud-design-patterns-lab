package com.example.infrastructure.web;

import com.example.domain.ShippingQuote;
import com.example.domain.ShippingQuoteGateway;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shipping-quotes")
public class ShippingQuoteController {

  private final ShippingQuoteGateway shippingQuoteGateway;

  public ShippingQuoteController(ShippingQuoteGateway shippingQuoteGateway) {
    this.shippingQuoteGateway = shippingQuoteGateway;
  }

  @GetMapping("/{orderId}")
  public ShippingQuote quote(@PathVariable("orderId") String orderId) {
    return this.shippingQuoteGateway.quote(orderId);
  }
}
