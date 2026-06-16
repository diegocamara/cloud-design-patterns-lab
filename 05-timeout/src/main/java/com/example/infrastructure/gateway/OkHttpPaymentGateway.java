package com.example.infrastructure.gateway;

import static com.example.infrastructure.utils.JsonUtils.readValue;
import static java.lang.String.format;

import com.example.domain.gateway.PaymentGateway;
import com.example.domain.gateway.model.PaymentGatewayResponse;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class OkHttpPaymentGateway implements PaymentGateway {

  private final OkHttpClient okHttpClient;
  private final String baseUrl;

  public OkHttpPaymentGateway(OkHttpClient okHttpClient, String baseUrl) {
    this.okHttpClient = okHttpClient;
    this.baseUrl = baseUrl;
  }

  @Override
  public PaymentGatewayResponse getPaymentStatus(String orderId) {
    final var request =
        new Request.Builder()
            .url(format("%s/payments/%s/status", this.baseUrl, orderId))
            .get()
            .build();

    try (final var response = this.okHttpClient.newCall(request).execute()) {

      if (!response.isSuccessful()) {
        throw new RuntimeException("Unexpected payment response: " + response.code());
      }

      return readValue(response.body().string(), PaymentGatewayResponse.class);

    } catch (IOException ioException) {
      throw new RuntimeException("Error calling payment service", ioException);
    }
  }
}
