package com.example.domain.gateway;

import java.math.BigDecimal;

public record ShippingQuoteRequest(
    String originZipCode,
    String destinationZipCode,
    BigDecimal packageWeightInKg,
    BigDecimal packageValue) {}
