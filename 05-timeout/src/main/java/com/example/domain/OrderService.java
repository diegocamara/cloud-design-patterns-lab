package com.example.domain;

import com.example.domain.model.OrderPaymentStatusResponse;

public interface OrderService {

  OrderPaymentStatusResponse getPaymentStatus(String orderId);
}
