package com.example.domain;

public interface ShippingQuoteGateway {
  ShippingQuote quote(String orderId);
}
