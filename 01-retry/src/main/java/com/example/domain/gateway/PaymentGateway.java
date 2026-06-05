package com.example.domain.gateway;

public interface PaymentGateway {

  PaymentResult process(PaymentRequest paymentRequest);
}
