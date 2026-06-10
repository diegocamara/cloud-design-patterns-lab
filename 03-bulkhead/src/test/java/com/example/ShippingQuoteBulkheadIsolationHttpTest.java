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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "server.tomcat.threads.max=10",
      "server.tomcat.threads.min-spare=10",
      "fake.quoteSleepInMillis=3000",
      "resilience4j.bulkhead.instances.shippingQuote.maxConcurrentCalls=2",
      "resilience4j.bulkhead.instances.shippingQuote.maxWaitDuration=0"
    })
public class ShippingQuoteBulkheadIsolationHttpTest {

  @Autowired private TestRestTemplate testRestTemplate;

  @Autowired private FakeShippingQuoteGateway fakeShippingQuoteGateway;

  @Test
  void shouldKeepStatusEndpointAvailableWhileShippingQuoteCapacityIsExhausted() {

    final var totalRequests = 10;
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

      ResponseEntity<String> statusResponse =
          this.testRestTemplate.getForEntity("/status", String.class);

      assertEquals(HttpStatus.OK, statusResponse.getStatusCode());
      assertEquals("OK", statusResponse.getBody());

      futures.forEach(
          future -> {
            try {
              future.get();
            } catch (Exception exception) {
              throw new RuntimeException(exception);
            }
          });

      assertEquals(2, this.fakeShippingQuoteGateway.startedCalls());
      assertEquals(8, this.fakeShippingQuoteGateway.fallbackCalls());
    }
  }
}
