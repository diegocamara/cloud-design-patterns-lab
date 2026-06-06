package com.example.domain.gateway;

import java.math.BigDecimal;

public record ShippingQuote(
    String quoteId, String carrier, BigDecimal price, int estimatedBusinessDays) {}
