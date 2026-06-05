package com.example.infrastructure.gateway;

import static com.example.infrastructure.utils.JsonUtils.readValue;
import static com.example.infrastructure.utils.JsonUtils.writeValueAsString;

import com.example.domain.gateway.PaymentGateway;
import com.example.domain.gateway.PaymentRequest;
import com.example.domain.gateway.PaymentResult;
import java.io.IOException;
import java.util.Set;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class OkHttpPaymentGateway implements PaymentGateway {

  private final MediaType JSON = MediaType.get("application/json");
  private final Set<Integer> PAYMENT_PROVIDER_UNAVAILABLE_STATUS_CODES = Set.of(500, 502, 503, 504);
  private final Set<Integer> PAYMENT_REJECTED_STATUS_CODES = Set.of(400, 401, 402, 403, 404, 422);

  private final OkHttpClient okHttpClient;
  private final String baseUrl;

  public OkHttpPaymentGateway(OkHttpClient okHttpClient, String baseUrl) {
    this.okHttpClient = okHttpClient;
    this.baseUrl = baseUrl;
  }

  @Override
  public PaymentResult process(PaymentRequest paymentRequest) {

    final var body = RequestBody.create(writeValueAsString(paymentRequest), JSON);

    final var request = new Request.Builder().url(this.baseUrl).post(body).build();

    try (final var response = this.okHttpClient.newCall(request).execute()) {

      final var statusCode = response.code();

      if (PAYMENT_PROVIDER_UNAVAILABLE_STATUS_CODES.contains(statusCode)) {
        throw new PaymentProviderUnavailableException(
            "Payment provider unavailable. Status: " + statusCode);
      }

      if (PAYMENT_REJECTED_STATUS_CODES.contains(statusCode)) {
        throw new PaymentRejectedException("Payment rejected by provider. Status: " + statusCode);
      }

      if (!response.isSuccessful()) {
        throw new PaymentProviderException(
            "Unexpected payment provider response. Status: " + statusCode);
      }
      return readValue(response.body().string(), PaymentResult.class);
    } catch (IOException ioException) {
      throw new PaymentProviderUnavailableException(
          "Payment provider unavailable due to network error", ioException);
    }
  }

  public static class PaymentProviderException extends RuntimeException {

    public PaymentProviderException(String message) {
      super(message);
    }

    public PaymentProviderException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static class PaymentProviderUnavailableException extends PaymentProviderException {

    public PaymentProviderUnavailableException(String message) {
      super(message);
    }

    public PaymentProviderUnavailableException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static class PaymentRejectedException extends RuntimeException {

    public PaymentRejectedException(String message) {
      super(message);
    }
  }
}
