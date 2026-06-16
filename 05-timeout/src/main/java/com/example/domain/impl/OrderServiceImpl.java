package com.example.domain.impl;

import com.example.domain.OrderService;
import com.example.domain.gateway.PaymentGateway;
import com.example.domain.model.OrderPaymentStatusResponse;

public class OrderServiceImpl implements OrderService {

  private final PaymentGateway paymentGateway;

  public OrderServiceImpl(PaymentGateway paymentGateway) {
    this.paymentGateway = paymentGateway;
  }

  @Override
  public OrderPaymentStatusResponse getPaymentStatus(String orderId) {
    final var paymentGatewayResponse = this.paymentGateway.getPaymentStatus(orderId);
    return new OrderPaymentStatusResponse(
        orderId, paymentGatewayResponse.status(), "Payment status retrieved successfully");
  }
}
