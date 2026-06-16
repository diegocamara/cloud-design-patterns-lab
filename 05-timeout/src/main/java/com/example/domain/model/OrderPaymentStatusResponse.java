package com.example.domain.model;

public record OrderPaymentStatusResponse(String orderId, String paymentStatus, String message) {}
