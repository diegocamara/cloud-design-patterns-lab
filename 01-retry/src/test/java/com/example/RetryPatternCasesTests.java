package com.example;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

import com.example.domain.gateway.PaymentGateway;
import com.example.domain.gateway.PaymentRequest;
import com.example.domain.gateway.PaymentStatus;
import com.example.infrastructure.gateway.OkHttpPaymentGateway;
import com.example.infrastructure.utils.OkHttpUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RetryPatternCasesTests {

  public static final String PAYMENTS_URL = "/payments";
  @RegisterExtension static WireMockExtension wireMockExtension = new WireMockExtension();

  private final WireMockServer paymentsServer;
  private final PaymentGateway paymentsGateway;

  public RetryPatternCasesTests(WireMockServer wireMockServer) {
    this.paymentsServer = wireMockServer;
    this.paymentsGateway =
        new OkHttpPaymentGateway(
            OkHttpUtils.okHttpClient(), paymentsServer.baseUrl() + PAYMENTS_URL);
  }

  @Test
  void shouldThrowExceptionWhenPaymentProviderFailsWithoutRetry() {

    this.paymentsServer.stubFor(post(PAYMENTS_URL).willReturn(serverError()));

    var request =
        new PaymentRequest(
            UUID.randomUUID(),
            new BigDecimal("199.90"),
            Currency.getInstance("BRL"),
            "order-" + UUID.randomUUID());

    Assertions.assertThrows(
        OkHttpPaymentGateway.PaymentProviderUnavailableException.class,
        () -> paymentsGateway.process(request));

    this.paymentsServer.verify(1, postRequestedFor(urlEqualTo(PAYMENTS_URL)));
  }

  @Test
  void shouldProcessPaymentWhenTemporaryFailureIsRecoveredWithRetry() {

    this.paymentsServer.stubFor(
        post(PAYMENTS_URL)
            .inScenario("Payment provider temporary failure")
            .whenScenarioStateIs(STARTED)
            .willReturn(serverError())
            .willSetStateTo("second attempt"));

    this.paymentsServer.stubFor(
        post(PAYMENTS_URL)
            .inScenario("Payment provider temporary failure")
            .whenScenarioStateIs("second attempt")
            .willReturn(serverError())
            .willSetStateTo("third attempt"));

    this.paymentsServer.stubFor(
        post(PAYMENTS_URL)
            .inScenario("Payment provider temporary failure")
            .whenScenarioStateIs("third attempt")
            .willReturn(
                okJson(
                    """
                                    {
                                      "transactionId": "tx-123",
                                      "status": "APPROVED"
                                    }
                                    """)));

    final var retryConfig =
        RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            .retryExceptions(OkHttpPaymentGateway.PaymentProviderUnavailableException.class)
            .build();

    final var retry = Retry.of("payment-provider", retryConfig);

    PaymentGateway paymentGatewayWithRetry =
        paymentRequest ->
            Retry.decorateSupplier(retry, () -> paymentsGateway.process(paymentRequest)).get();

    var request =
        new PaymentRequest(
            UUID.randomUUID(),
            new BigDecimal("199.90"),
            Currency.getInstance("BRL"),
            "order-" + UUID.randomUUID());

    final var result = paymentGatewayWithRetry.process(request);

    Assertions.assertEquals("tx-123", result.transactionId());
    Assertions.assertEquals(PaymentStatus.APPROVED, result.status());

    this.paymentsServer.verify(3, postRequestedFor(urlEqualTo(PAYMENTS_URL)));
  }

  @Test
  void shouldThrowExceptionWhenAllRetryAttemptsAreExhausted() {

    this.paymentsServer.stubFor(post(urlEqualTo(PAYMENTS_URL)).willReturn(serverError()));

    final var retryConfig =
        RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            .retryExceptions(OkHttpPaymentGateway.PaymentProviderUnavailableException.class)
            .build();

    final var retry = Retry.of("payment-provider", retryConfig);

    PaymentGateway paymentGatewayWithRetry =
        paymentRequest ->
            Retry.decorateSupplier(retry, () -> paymentsGateway.process(paymentRequest)).get();

    var request =
        new PaymentRequest(
            UUID.randomUUID(),
            new BigDecimal("199.90"),
            Currency.getInstance("BRL"),
            "order-" + UUID.randomUUID());

    Assertions.assertThrows(
        OkHttpPaymentGateway.PaymentProviderUnavailableException.class,
        () -> paymentGatewayWithRetry.process(request));

    this.paymentsServer.verify(3, postRequestedFor(urlEqualTo(PAYMENTS_URL)));
  }

  @Test
  void shouldNotRetryWhenPaymentIsRejectedByProvider() {

    this.paymentsServer.stubFor(post(urlEqualTo(PAYMENTS_URL)).willReturn(status(400)));

    final var retryConfig =
        RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            .retryExceptions(OkHttpPaymentGateway.PaymentProviderUnavailableException.class)
            .ignoreExceptions(OkHttpPaymentGateway.PaymentRejectedException.class)
            .build();

    final var retry = Retry.of("payment-provider", retryConfig);

    PaymentGateway paymentGatewayWithRetry =
        paymentRequest ->
            Retry.decorateSupplier(retry, () -> paymentsGateway.process(paymentRequest)).get();

    var request =
        new PaymentRequest(
            UUID.randomUUID(),
            new BigDecimal("199.90"),
            Currency.getInstance("BRL"),
            "order-" + UUID.randomUUID());

    Assertions.assertThrows(
        OkHttpPaymentGateway.PaymentRejectedException.class,
        () -> paymentGatewayWithRetry.process(request));

    this.paymentsServer.verify(1, postRequestedFor(urlEqualTo(PAYMENTS_URL)));
  }

  @Test
  void shouldSendSameIdempotencyKeyOnEveryRetryAttempt() {

    this.paymentsServer.stubFor(
        post(PAYMENTS_URL)
            .inScenario("Payment provider temporary failure")
            .whenScenarioStateIs(STARTED)
            .willReturn(serverError())
            .willSetStateTo("second attempt"));

    this.paymentsServer.stubFor(
        post(PAYMENTS_URL)
            .inScenario("Payment provider temporary failure")
            .whenScenarioStateIs("second attempt")
            .willReturn(serverError())
            .willSetStateTo("third attempt"));

    this.paymentsServer.stubFor(
        post(PAYMENTS_URL)
            .inScenario("Payment provider temporary failure")
            .whenScenarioStateIs("third attempt")
            .willReturn(
                okJson(
                    """
                                                    {
                                                      "transactionId": "tx-123",
                                                      "status": "APPROVED"
                                                    }
                                                    """)));

    final var retryConfig =
        RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            .retryExceptions(OkHttpPaymentGateway.PaymentProviderUnavailableException.class)
            .ignoreExceptions(OkHttpPaymentGateway.PaymentRejectedException.class)
            .build();

    final var retry = Retry.of("payment-provider", retryConfig);

    PaymentGateway paymentGatewayWithRetry =
        paymentRequest ->
            Retry.decorateSupplier(retry, () -> paymentsGateway.process(paymentRequest)).get();

    var request =
        new PaymentRequest(
            UUID.randomUUID(),
            new BigDecimal("199.90"),
            Currency.getInstance("BRL"),
            "order-" + UUID.randomUUID());

    final var result = paymentGatewayWithRetry.process(request);

    Assertions.assertEquals("tx-123", result.transactionId());
    Assertions.assertEquals(PaymentStatus.APPROVED, result.status());

    this.paymentsServer.verify(
        3,
        postRequestedFor(urlEqualTo(PAYMENTS_URL))
            .withRequestBody(
                matchingJsonPath("$.idempotencyKey", equalTo(request.idempotencyKey()))));
  }
}
