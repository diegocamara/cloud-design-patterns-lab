package com.example.infrastructure.gateway;

import com.example.domain.ShippingQuote;
import com.example.domain.ShippingQuoteGateway;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FakeShippingQuoteGateway implements ShippingQuoteGateway {

  @Value("${fake.quoteSleepInMillis:3000}")
  private long quoteSleepInMillis;

  private final AtomicInteger startedCalls = new AtomicInteger();
  private final AtomicInteger fallbackCalls = new AtomicInteger();

  @Override
  @Bulkhead(name = "shippingQuote", fallbackMethod = "fallback")
  public ShippingQuote quote(String orderId) {
    this.startedCalls.incrementAndGet();
    final var currentThreadName = Thread.currentThread().getName();
    System.out.printf("%s started shipping quote for %s\n", currentThreadName, orderId);
    sleep(this.quoteSleepInMillis);
    System.out.printf("%s finished shipping quote for %s\n", currentThreadName, orderId);
    return new ShippingQuote(orderId, true, "Shipping quote calculated");
  }

  private ShippingQuote fallback(String orderId, Throwable throwable) {
    this.fallbackCalls.incrementAndGet();
    final var currentThreadName = Thread.currentThread().getName();
    System.out.printf(
        "%s rejected shipping quote for %s - %s\n",
        currentThreadName, orderId, throwable.getClass().getSimpleName());
    return new ShippingQuote(orderId, false, "Shipping quote unavailable");
  }

  public int startedCalls() {
    return this.startedCalls.get();
  }

  public int fallbackCalls() {
    return this.fallbackCalls.get();
  }

  public void reset() {
    this.startedCalls.set(0);
    this.fallbackCalls.set(0);
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Thread was interrupted", interruptedException);
    }
  }
}
