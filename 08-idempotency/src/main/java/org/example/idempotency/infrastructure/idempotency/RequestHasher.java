package org.example.idempotency.infrastructure.idempotency;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class RequestHasher {

  private static final String HASH_ALGORITHM = "SHA-256";

  private final ObjectMapper objectMapper;

  public RequestHasher(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String hash(Object request) {
    if (request == null) {
      throw new IllegalArgumentException("Request must not be null");
    }

    try {
      byte[] requestBytes = objectMapper.writeValueAsBytes(request);

      MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

      byte[] hash = digest.digest(requestBytes);

      return HexFormat.of().formatHex(hash);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Failed to serialize request", exception);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm is not available", exception);
    }
  }
}
