package com.example.infrastructure.gateway;

import static com.example.infrastructure.utils.JsonUtils.readValue;
import static com.example.infrastructure.utils.JsonUtils.writeValueAsString;
import static java.lang.String.format;

import com.example.domain.gateway.PaymentGateway;
import com.example.domain.gateway.PaymentRequest;
import com.example.domain.gateway.PaymentResult;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class OkHttpPaymentGateway implements PaymentGateway {

  private final MediaType JSON = MediaType.get("application/json");

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

      if (response.isSuccessful()) {
        return readValue(response.body().string(), PaymentResult.class);
      }
      throw new PaymentProviderUnavailableException(response.code());
    } catch (IOException ioException) {
      throw new RuntimeException(ioException);
    }
  }

  public static class PaymentProviderUnavailableException extends RuntimeException {

    public PaymentProviderUnavailableException(int code) {
      super(format("Payment provider unavailable!\ncode: %d", code));
    }
  }
}
