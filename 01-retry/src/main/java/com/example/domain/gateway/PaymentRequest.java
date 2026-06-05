package com.example.domain.gateway;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

public record PaymentRequest(
    UUID orderId, BigDecimal amount, Currency currency, String idempotencyKey) {}
