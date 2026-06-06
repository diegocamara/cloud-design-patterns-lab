package com.example;

import static com.example.infrastructure.utils.OkHttpUtils.okHttpClient;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.domain.gateway.ShippingQuoteGateway;
import com.example.domain.gateway.ShippingQuoteRequest;
import com.example.infrastructure.gateway.FallbackShippingQuoteGateway;
import com.example.infrastructure.gateway.OkHttpShippingQuoteGateway;
import com.example.infrastructure.gateway.Resilience4jCircuitBreakerShippingQuoteGateway;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CircuitBreakerPatternCasesTests {

  private final String SHIPPING_QUOTE_URL = "/shipping/quote";
  @RegisterExtension static WireMockExtension wireMockExtension = new WireMockExtension();

  private final WireMockServer shippingServer;
  private final ShippingQuoteGateway shippingQuoteGateway;

  public CircuitBreakerPatternCasesTests(WireMockServer wireMockServer) {
    this.shippingServer = wireMockServer;
    this.shippingQuoteGateway =
        new OkHttpShippingQuoteGateway(
            okHttpClient(), wireMockServer.baseUrl() + SHIPPING_QUOTE_URL);
  }

  @Test
  void shouldKeepCallingShippingProviderWhenCircuitBreakerIsNotUsed() {

    this.shippingServer.stubFor(post(urlEqualTo(SHIPPING_QUOTE_URL)).willReturn(serverError()));

    final var request =
        new ShippingQuoteRequest(
            "00000-000", "00000-000", new BigDecimal("2.5"), new BigDecimal("350.00"));

    IntStream.range(0, 5)
        .forEach(
            value ->
                assertThrows(
                    OkHttpShippingQuoteGateway.ShippingQuoteGatewayException.class,
                    () -> this.shippingQuoteGateway.quote(request)));

    this.shippingServer.verify(5, postRequestedFor(urlEqualTo(SHIPPING_QUOTE_URL)));
  }

  @Test
  void shouldStopCallingShippingProviderWhenCircuitBreakerIsOpen() {

    this.shippingServer.stubFor(post(SHIPPING_QUOTE_URL).willReturn(serverError()));

    final var circuitBreakerConfig =
        CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(2)
            .minimumNumberOfCalls(2)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(1)
            .build();

    final var circuitBreaker = CircuitBreaker.of("shippingQuoteGateway", circuitBreakerConfig);

    final var gateway =
        new Resilience4jCircuitBreakerShippingQuoteGateway(
            this.shippingQuoteGateway, circuitBreaker);

    final var request =
        new ShippingQuoteRequest(
            "00000-000", "00000-000", new BigDecimal("2.5"), new BigDecimal("350.00"));

    assertThrows(
        OkHttpShippingQuoteGateway.ShippingQuoteGatewayException.class,
        () -> gateway.quote(request));

    assertThrows(
        OkHttpShippingQuoteGateway.ShippingQuoteGatewayException.class,
        () -> gateway.quote(request));

    assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

    assertThrows(CallNotPermittedException.class, () -> gateway.quote(request));

    assertThrows(CallNotPermittedException.class, () -> gateway.quote(request));

    assertThrows(CallNotPermittedException.class, () -> gateway.quote(request));

    this.shippingServer.verify(2, postRequestedFor(urlEqualTo(SHIPPING_QUOTE_URL)));
  }

  @Test
  @SneakyThrows
  void shouldCloseCircuitBreakerAfterSuccessfulCallInHalfOpenState() {

    this.shippingServer.stubFor(
        post(urlEqualTo(SHIPPING_QUOTE_URL))
            .inScenario("Shipping provider recovery")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(serverError())
            .willSetStateTo("second failure"));

    this.shippingServer.stubFor(
        post(urlEqualTo(SHIPPING_QUOTE_URL))
            .inScenario("Shipping provider recovery")
            .whenScenarioStateIs("second failure")
            .willReturn(serverError())
            .willSetStateTo("recovered"));

    this.shippingServer.stubFor(
        post(urlEqualTo(SHIPPING_QUOTE_URL))
            .inScenario("Shipping provider recovery")
            .whenScenarioStateIs("recovered")
            .willReturn(
                okJson(
                    """
        {
          "quoteId": "quote-123",
          "carrier": "FastShip",
          "price": 32.90,
          "estimatedBusinessDays": 5
        }
        """)));

    final var circuitBreakerConfig =
        CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(2)
            .minimumNumberOfCalls(2)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMillis(300))
            .permittedNumberOfCallsInHalfOpenState(1)
            .build();

    final var circuitBreaker = CircuitBreaker.of("shippingQuoteGateway", circuitBreakerConfig);

    final var circuitBreakerGateway =
        new Resilience4jCircuitBreakerShippingQuoteGateway(
            this.shippingQuoteGateway, circuitBreaker);

    final var request =
        new ShippingQuoteRequest(
            "00000-000", "00000-000", new BigDecimal("2.5"), new BigDecimal("350.00"));

    assertThrows(
        OkHttpShippingQuoteGateway.ShippingQuoteGatewayException.class,
        () -> circuitBreakerGateway.quote(request));

    assertThrows(
        OkHttpShippingQuoteGateway.ShippingQuoteGatewayException.class,
        () -> circuitBreakerGateway.quote(request));

    assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

    Thread.sleep(400);

    final var quote = circuitBreakerGateway.quote(request);

    assertEquals("quote-123", quote.quoteId());
    assertEquals("FastShip", quote.carrier());
    assertEquals(new BigDecimal("32.90"), quote.price());
    assertEquals(5, quote.estimatedBusinessDays());

    assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());

    this.shippingServer.verify(3, postRequestedFor(urlEqualTo(SHIPPING_QUOTE_URL)));
  }

  @Test
  void shouldOpenCircuitBreakerWhenShippingProviderIsTooSlow() {

    this.shippingServer.stubFor(
        post(urlEqualTo(SHIPPING_QUOTE_URL))
            .willReturn(
                okJson(
                        """
                    {
                      "quoteId": "quote-123",
                      "carrier": "FastShip",
                      "price": 32.90,
                      "estimatedBusinessDays": 5
                    }
                    """)
                    .withFixedDelay(500)));

    final var circuitBreakerConfig =
        CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(2)
            .minimumNumberOfCalls(2)
            .failureRateThreshold(100)
            .slowCallDurationThreshold(Duration.ofMillis(200))
            .slowCallRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(1)
            .build();

    final var circuitBreaker = CircuitBreaker.of("shippingQuoteGateway", circuitBreakerConfig);

    final var circuitBreakerGateway =
        new Resilience4jCircuitBreakerShippingQuoteGateway(
            this.shippingQuoteGateway, circuitBreaker);

    final var request =
        new ShippingQuoteRequest(
            "00000-000", "00000-000", new BigDecimal("2.5"), new BigDecimal("350.00"));

    final var firstQuote = circuitBreakerGateway.quote(request);
    final var secondQuote = circuitBreakerGateway.quote(request);

    assertEquals("quote-123", firstQuote.quoteId());
    assertEquals("quote-123", secondQuote.quoteId());

    assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

    assertThrows(CallNotPermittedException.class, () -> circuitBreakerGateway.quote(request));

    assertThrows(CallNotPermittedException.class, () -> circuitBreakerGateway.quote(request));

    this.shippingServer.verify(2, postRequestedFor(urlEqualTo(SHIPPING_QUOTE_URL)));
  }

  @Test
  void shouldUseFallbackGatewayWhenCircuitBreakerIsOpen() {

    final var fastShipUrl = "/fastship/shipping/quote";
    final var safeCarrierUrl = "/safecarrier/shipping/quote";

    this.shippingServer.stubFor(post(urlEqualTo(fastShipUrl)).willReturn(serverError()));

    this.shippingServer.stubFor(
        post(urlEqualTo(safeCarrierUrl))
            .willReturn(
                okJson(
                    """
                    {
                      "quoteId": "fallback-quote-123",
                      "carrier": "SafeCarrier",
                      "price": 45.90,
                      "estimatedBusinessDays": 7
                    }
                    """)));

    final var circuitBreakerConfig =
        CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(2)
            .minimumNumberOfCalls(2)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(1)
            .build();

    final var circuitBreaker = CircuitBreaker.of("shippingQuoteGateway", circuitBreakerConfig);

    final var fastShipGateway =
        new OkHttpShippingQuoteGateway(okHttpClient(), this.shippingServer.baseUrl() + fastShipUrl);

    final var circuitBreakerGateway =
        new Resilience4jCircuitBreakerShippingQuoteGateway(fastShipGateway, circuitBreaker);

    final var safeCarrierGateway =
        new OkHttpShippingQuoteGateway(
            okHttpClient(), this.shippingServer.baseUrl() + safeCarrierUrl);

    final var fallbackGateway =
        new FallbackShippingQuoteGateway(circuitBreakerGateway, safeCarrierGateway);

    final var request =
        new ShippingQuoteRequest(
            "00000-000", "00000-000", new BigDecimal("2.5"), new BigDecimal("350.00"));

    assertThrows(
        OkHttpShippingQuoteGateway.ShippingQuoteGatewayException.class,
        () -> fallbackGateway.quote(request));

    assertThrows(
        OkHttpShippingQuoteGateway.ShippingQuoteGatewayException.class,
        () -> fallbackGateway.quote(request));

    assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

    final var quote = fallbackGateway.quote(request);

    assertEquals("fallback-quote-123", quote.quoteId());
    assertEquals("SafeCarrier", quote.carrier());
    assertEquals(new BigDecimal("45.90"), quote.price());
    assertEquals(7, quote.estimatedBusinessDays());

    this.shippingServer.verify(2, postRequestedFor(urlEqualTo(fastShipUrl)));

    this.shippingServer.verify(1, postRequestedFor(urlEqualTo(safeCarrierUrl)));
  }
}
