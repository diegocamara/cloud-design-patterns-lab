package org.example.gameprogressionreader.infrastructure.messaging;

import com.mongodb.DuplicateKeyException;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.PlayerCreatedMessage;
import org.example.StageCompletedMessage;
import org.example.gameprogressionreader.infrastructure.messaging.exceptions.MissingEventTypeHeaderException;
import org.example.gameprogressionreader.infrastructure.processor.GameProgressionProjectionProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class KafkaGameProgressingEventsConsumer {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(KafkaGameProgressingEventsConsumer.class);

  private static final String EVENT_TYPE_HEADER = "eventType";

  private static final String PLAYER_CREATED_MESSAGE = "PlayerCreatedMessage";

  private static final String STAGE_COMPLETED_MESSAGE = "StageCompletedMessage";

  private final ObjectMapper objectMapper;
  private final GameProgressionProjectionProcessor gameProgressionProjectionProcessor;

  public KafkaGameProgressingEventsConsumer(
      ObjectMapper objectMapper,
      GameProgressionProjectionProcessor gameProgressionProjectionProcessor) {
    this.objectMapper = objectMapper;
    this.gameProgressionProjectionProcessor = gameProgressionProjectionProcessor;
  }

  @KafkaListener(topics = {"${app.kafka.topic}"})
  public void consume(ConsumerRecord<String, String> record) {

    final var eventType = getEventType(record);

    try {
      switch (eventType) {
        case PLAYER_CREATED_MESSAGE -> consumePlayerCreatedMessage(record.value());

        case STAGE_COMPLETED_MESSAGE -> consumeStageCompletedMessage(record.value());
      }
    } catch (DuplicateKeyException duplicateKeyException) {
      LOGGER.warn("Duplicate key found in record: {}", record.value());
    }
  }

  private void consumePlayerCreatedMessage(String payload) {
    final var playerCreatedMessage = deserialize(payload, PlayerCreatedMessage.class);
    this.gameProgressionProjectionProcessor.process(playerCreatedMessage);
  }

  private void consumeStageCompletedMessage(String payload) {
    final var stageCompletedMessage = deserialize(payload, StageCompletedMessage.class);
    this.gameProgressionProjectionProcessor.process(stageCompletedMessage);
  }

  private String getEventType(ConsumerRecord<String, String> record) {
    final var header = record.headers().lastHeader(EVENT_TYPE_HEADER);

    if (header == null || header.value() == null) {
      throw new MissingEventTypeHeaderException();
    }

    return new String(header.value(), StandardCharsets.UTF_8);
  }

  private <T> T deserialize(String payload, Class<T> messageType) {
    try {
      return this.objectMapper.readValue(payload, messageType);
    } catch (JacksonException jacksonException) {
      throw new RuntimeException(
          "Could not deserialize event payload as " + messageType.getSimpleName(),
          jacksonException);
    }
  }
}
