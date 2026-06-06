package com.example.infrastructure.gateway;

import static com.example.infrastructure.utils.JsonUtils.readValue;
import static com.example.infrastructure.utils.JsonUtils.writeValueAsString;

import com.example.domain.gateway.ShippingQuote;
import com.example.domain.gateway.ShippingQuoteGateway;
import com.example.domain.gateway.ShippingQuoteRequest;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class OkHttpShippingQuoteGateway implements ShippingQuoteGateway {

  private final MediaType JSON = MediaType.get("application/json");

  private final OkHttpClient okHttpClient;
  private final String baseUrl;

  public OkHttpShippingQuoteGateway(OkHttpClient okHttpClient, String baseUrl) {
    this.okHttpClient = okHttpClient;
    this.baseUrl = baseUrl;
  }

  @Override
  public ShippingQuote quote(ShippingQuoteRequest shippingQuoteRequest) {

    final var body = RequestBody.create(writeValueAsString(shippingQuoteRequest), JSON);

    final var request = new Request.Builder().url(this.baseUrl).post(body).build();

    final var call = this.okHttpClient.newCall(request);

    try (final var response = call.execute()) {

      if (!response.isSuccessful()) {
        throw new ShippingQuoteGatewayException(
            "Shipping quote provider returned an unsuccessful response. HTTP status: "
                + response.code());
      }

      return readValue(response.body().string(), ShippingQuote.class);
    } catch (IOException ioException) {
      throw new ShippingQuoteGatewayException(
          "Unable to communicate with shipping quote provider", ioException);
    }
  }

  public static class ShippingQuoteGatewayException extends RuntimeException {

    public ShippingQuoteGatewayException(String message) {
      super(message);
    }

    public ShippingQuoteGatewayException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
