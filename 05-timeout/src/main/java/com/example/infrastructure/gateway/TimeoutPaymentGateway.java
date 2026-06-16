package com.example.infrastructure.gateway;

import com.example.domain.gateway.PaymentGateway;
import com.example.domain.gateway.model.PaymentGatewayResponse;
import com.example.infrastructure.exception.PaymentGatewayTimeoutException;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class TimeoutPaymentGateway implements PaymentGateway {

  private final PaymentGateway delegate;
  private final TimeLimiter timeLimiter;
  private final ScheduledExecutorService timeoutScheduler;
  private final ExecutorService paymentGatewayExecutor;

  public TimeoutPaymentGateway(
      PaymentGateway delegate,
      TimeLimiter timeLimiter,
      ScheduledExecutorService timeoutScheduler,
      ExecutorService paymentGatewayExecutor) {
    this.delegate = delegate;
    this.timeLimiter = timeLimiter;
    this.timeoutScheduler = timeoutScheduler;
    this.paymentGatewayExecutor = paymentGatewayExecutor;
  }

  @Override
  public PaymentGatewayResponse getPaymentStatus(String orderId) {
    Supplier<CompletableFuture<PaymentGatewayResponse>> operation =
        () ->
            CompletableFuture.supplyAsync(
                () -> delegate.getPaymentStatus(orderId), this.paymentGatewayExecutor);

    try {
      return this.timeLimiter
          .executeCompletionStage(this.timeoutScheduler, operation)
          .toCompletableFuture()
          .join();
    } catch (CompletionException completionException) {

      if (isTimeout(completionException)) {
        throw new PaymentGatewayTimeoutException(
            "Payment service did not respond in time", completionException);
      }
      throw completionException;
    }
  }

  private boolean isTimeout(Throwable throwable) {
    Throwable current = throwable;

    while (current != null) {
      if (current instanceof TimeoutException) {
        return true;
      }

      current = current.getCause();
    }

    return false;
  }
}
