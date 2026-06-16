package com.example;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.domain.impl.OrderServiceImpl;
import com.example.infrastructure.exception.PaymentGatewayTimeoutException;
import com.example.infrastructure.gateway.OkHttpPaymentGateway;
import com.example.infrastructure.gateway.TimeoutPaymentGateway;
import com.example.infrastructure.utils.OkHttpUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TimeoutPatternCasesTests {

  @RegisterExtension static WireMockExtension wireMockExtension = new WireMockExtension();

  private final WireMockServer paymentServer;

  public TimeoutPatternCasesTests(WireMockServer wireMockServer) {
    this.paymentServer = wireMockServer;
  }

  @Test
  void shouldWaitForPaymentServiceWhenTimeoutPatternIsNotApplied() {

    this.paymentServer.stubFor(
        WireMock.get(urlEqualTo("/payments/123/status"))
            .willReturn(
                okJson(
                        """
        {
          "orderId": "123",
          "status": "APPROVED"
        }
    """)
                    .withFixedDelay(500)));

    final var okHttpPaymentGateway =
        new OkHttpPaymentGateway(OkHttpUtils.okHttpClient(), this.paymentServer.baseUrl());
    final var orderService = new OrderServiceImpl(okHttpPaymentGateway);

    final var result = orderService.getPaymentStatus("123");
    assertEquals("123", result.orderId());
    assertEquals("APPROVED", result.paymentStatus());
    assertEquals("Payment status retrieved successfully", result.message());
  }

  @Test
  void shouldReturnPaymentStatusWhenPaymentServiceRespondsBeforeTimeout() {

    this.paymentServer.stubFor(
        WireMock.get(urlEqualTo("/payments/123/status"))
            .willReturn(
                okJson(
                        """
        {
          "orderId": "123",
          "status": "APPROVED"
        }
    """)
                    .withFixedDelay(200)));

    final var okHttpPaymentGateway =
        new OkHttpPaymentGateway(OkHttpUtils.okHttpClient(), this.paymentServer.baseUrl());
    final var timeLimiter =
        TimeLimiter.of(
            TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(1000))
                .cancelRunningFuture(true)
                .build());
    final var timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
    final var paymentGatewayExecutor = Executors.newSingleThreadExecutor();

    try {
      final var timeoutPaymentGateway =
          new TimeoutPaymentGateway(
              okHttpPaymentGateway, timeLimiter, timeoutScheduler, paymentGatewayExecutor);
      final var orderService = new OrderServiceImpl(timeoutPaymentGateway);

      final var result = orderService.getPaymentStatus("123");

      assertEquals("123", result.orderId());
      assertEquals("APPROVED", result.paymentStatus());
      assertEquals("Payment status retrieved successfully", result.message());
    } finally {
      timeoutScheduler.shutdownNow();
      paymentGatewayExecutor.shutdownNow();
    }
  }

  @Test
  void shouldFailFastWhenPaymentServiceDoesNotRespondBeforeTimeout() {

    this.paymentServer.stubFor(
        WireMock.get(urlEqualTo("/payments/123/status"))
            .willReturn(
                okJson(
                        """
        {
          "orderId": "123",
          "status": "APPROVED"
        }
    """)
                    .withFixedDelay(1000)));

    final var okHttpPaymentGateway =
        new OkHttpPaymentGateway(OkHttpUtils.okHttpClient(), this.paymentServer.baseUrl());
    final var timeLimiter =
        TimeLimiter.of(
            TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(200))
                .cancelRunningFuture(true)
                .build());
    final var timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
    final var paymentGatewayExecutor = Executors.newSingleThreadExecutor();

    try {
      final var timeoutPaymentGateway =
          new TimeoutPaymentGateway(
              okHttpPaymentGateway, timeLimiter, timeoutScheduler, paymentGatewayExecutor);
      final var orderService = new OrderServiceImpl(timeoutPaymentGateway);

      final var exception =
          assertThrows(
              PaymentGatewayTimeoutException.class, () -> orderService.getPaymentStatus("123"));

      assertEquals("Payment service did not respond in time", exception.getMessage());
      assertTrue(hasCause(exception, TimeoutException.class));
    } finally {
      timeoutScheduler.shutdownNow();
      paymentGatewayExecutor.shutdownNow();
    }
  }

  @Test
  void shouldNotConvertPaymentServiceErrorsToTimeoutException() {

    this.paymentServer.stubFor(
        WireMock.get(urlEqualTo("/payments/123/status")).willReturn(serverError()));

    final var okHttpPaymentGateway =
        new OkHttpPaymentGateway(OkHttpUtils.okHttpClient(), this.paymentServer.baseUrl());
    final var timeLimiter =
        TimeLimiter.of(
            TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(1000))
                .cancelRunningFuture(true)
                .build());
    final var timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
    final var paymentGatewayExecutor = Executors.newSingleThreadExecutor();

    try {
      final var timeoutPaymentGateway =
          new TimeoutPaymentGateway(
              okHttpPaymentGateway, timeLimiter, timeoutScheduler, paymentGatewayExecutor);
      final var orderService = new OrderServiceImpl(timeoutPaymentGateway);

      final var exception =
          assertThrows(CompletionException.class, () -> orderService.getPaymentStatus("123"));

      assertEquals("Unexpected payment response: 500", exception.getCause().getMessage());
    } finally {
      timeoutScheduler.shutdownNow();
      paymentGatewayExecutor.shutdownNow();
    }
  }

  private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
    Throwable current = throwable;

    while (current != null) {
      if (causeType.isInstance(current)) {
        return true;
      }

      current = current.getCause();
    }

    return false;
  }
}
