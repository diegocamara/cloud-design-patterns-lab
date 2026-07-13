package org.example.gameprogressionwrite.infrastructure.scheduler;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.gameprogressionwrite.infrastructure.repository.springdatajpa.SpringDataJpaOutboxEventRepository;
import org.example.gameprogressionwrite.infrastructure.repository.springdatajpa.model.OutboxEventEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxEventsScheduler {

  public static final String EVENT_ID_MESSAGE_HEADER = "eventId";
  public static final String EVENT_TYPE_MESSAGE_HEADER = "eventType";
  public static final int KAFKA_SEND_TIMEOUT_SECONDS = 10;
  private final SpringDataJpaOutboxEventRepository springDataJpaOutboxEventRepository;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final Clock clock;
  private final String topic;
  private final int batchSize;
  private final int maxAttempts;

  public OutboxEventsScheduler(
      SpringDataJpaOutboxEventRepository springDataJpaOutboxEventRepository,
      KafkaTemplate<String, String> kafkaTemplate,
      Clock clock,
      @Value("${app.kafka.topic}") String topic,
      @Value("${app.outbox.batch-size:100}") int batchSize,
      @Value("${app.outbox.max-attempts:5}") int maxAttempts) {
    this.springDataJpaOutboxEventRepository = springDataJpaOutboxEventRepository;
    this.kafkaTemplate = kafkaTemplate;
    this.clock = clock;
    this.topic = topic;
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
  }

  @Transactional
  @Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms:1000}")
  public void publishPendingEvents() {
    final var events =
        this.springDataJpaOutboxEventRepository.findNextBatch(this.batchSize, this.maxAttempts);

    events.forEach(this::publish);
  }

  private void publish(OutboxEventEntity event) {
    try {
      final var record = createProducerRecord(event);
      this.kafkaTemplate.send(record).get(KAFKA_SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      event.markAsPublished(this.clock.instant());
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Outbox publication was interrupted", interruptedException);
    } catch (ExecutionException | TimeoutException exception) {
      event.registerFailure(getErrorMessage(exception));
    }
  }

  private ProducerRecord<String, String> createProducerRecord(OutboxEventEntity event) {
    final var record =
        new ProducerRecord<>(this.topic, event.getAggregateId().toString(), event.getPayload());

    record
        .headers()
        .add(EVENT_ID_MESSAGE_HEADER, event.getId().toString().getBytes(StandardCharsets.UTF_8));

    record
        .headers()
        .add(EVENT_TYPE_MESSAGE_HEADER, event.getEventType().getBytes(StandardCharsets.UTF_8));

    return record;
  }

  private String getErrorMessage(Exception exception) {
    final var cause = exception.getCause();
    if (cause != null && cause.getMessage() != null) {
      return cause.getMessage();
    }
    return exception.getMessage();
  }
}
