package com.example.domain.gateway;

public record PaymentResult(String transactionId, PaymentStatus status) {}
