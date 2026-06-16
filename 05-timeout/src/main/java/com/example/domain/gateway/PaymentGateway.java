package com.example.domain.gateway;

import com.example.domain.gateway.model.PaymentGatewayResponse;

public interface PaymentGateway {
  PaymentGatewayResponse getPaymentStatus(String orderId);
}
