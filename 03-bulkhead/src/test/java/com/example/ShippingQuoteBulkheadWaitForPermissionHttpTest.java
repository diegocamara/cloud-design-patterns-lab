package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.domain.ShippingQuote;
import com.example.infrastructure.gateway.FakeShippingQuoteGateway;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "server.tomcat.threads.max=10",
      "server.tomcat.threads.min-spare=10",
      "fake.quoteSleepInMillis=1000",
      "resilience4j.bulkhead.instances.shippingQuote.maxConcurrentCalls=2",
      "resilience4j.bulkhead.instances.shippingQuote.maxWaitDuration=1500ms"
    })
public class ShippingQuoteBulkheadWaitForPermissionHttpTest {

  @Autowired private TestRestTemplate testRestTemplate;

  @Autowired private FakeShippingQuoteGateway fakeShippingQuoteGateway;

  @Test
  void shouldProcessRequestsWhenPermissionBecomesAvailableWithinWaitDuration() {

    final var totalRequests = 4;
    final var startSignal = new CountDownLatch(1);

    List<Callable<ShippingQuote>> tasks =
        IntStream.rangeClosed(1, totalRequests)
            .mapToObj(
                value ->
                    (Callable<ShippingQuote>)
                        () -> {
                          startSignal.await();

                          ResponseEntity<ShippingQuote> response =
                              this.testRestTemplate.getForEntity(
                                  "/shipping-quotes/order-" + value, ShippingQuote.class);
                          return response.getBody();
                        })
            .toList();

    try (ExecutorService executorService = Executors.newFixedThreadPool(totalRequests)) {

      final var futures = tasks.stream().map(executorService::submit).toList();

      startSignal.countDown();

      final var results =
          futures.stream()
              .map(
                  future -> {
                    try {
                      return future.get();
                    } catch (Exception exception) {
                      throw new RuntimeException(exception);
                    }
                  })
              .toList();

      final var availableQuotes = results.stream().filter(ShippingQuote::available).count();
      final var unavailableQuotes = results.stream().filter(result -> !result.available()).count();

      assertEquals(4, availableQuotes);
      assertEquals(0, unavailableQuotes);

      assertEquals(4, this.fakeShippingQuoteGateway.startedCalls());
      assertEquals(0, this.fakeShippingQuoteGateway.fallbackCalls());
    }
  }
}
