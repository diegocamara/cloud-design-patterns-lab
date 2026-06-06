package com.example.infrastructure.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.SneakyThrows;

public class JsonUtils {

  private static volatile ObjectMapper objectMapper;

  public static ObjectMapper objectMapper() {

    if (objectMapper == null) {

      synchronized (JsonUtils.class) {
        if (objectMapper == null) {

          objectMapper = JsonMapper.builder().findAndAddModules().build();
        }
      }
    }

    return objectMapper;
  }

  @SneakyThrows
  public static String writeValueAsString(Object value) {
    return objectMapper().writeValueAsString(value);
  }

  @SneakyThrows
  public static <T> T readValue(String content, Class<T> valueType) {
    return objectMapper().readValue(content, valueType);
  }
}
