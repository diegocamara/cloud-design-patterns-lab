package com.example.infrastructure.exception;

public class PaymentGatewayTimeoutException extends RuntimeException {

  public PaymentGatewayTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}
