package org.example.integration;

import static com.mongodb.client.model.Filters.eq;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.restassured.http.ContentType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bson.Document;
import org.bson.types.Binary;
import org.example.integration.environment.DockerComposeEnvironmentExtension;
import org.example.integration.kafka.KafkaClient;
import org.example.integration.kafka.KafkaExtension;
import org.example.integration.mongo.MongoClient;
import org.example.integration.mongo.MongoExtension;
import org.example.integration.postgres.PostgresExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Stage completed event idempotency")
@ExtendWith({
  DockerComposeEnvironmentExtension.class,
  PostgresExtension.class,
  MongoExtension.class,
  KafkaExtension.class
})
class StageCompletedIdempotencyIntegrationTest {

  private static final String NICKNAME = "idempotent-player";
  private static final String STAGE_CODE = "idempotent-stage";
  private static final int XP_GAINED = 750;

  @Test
  @DisplayName("does not duplicate completed stages when the same event is consumed twice")
  void shouldNotDuplicateCompletedStageWhenStageCompletedEventIsDeliveredAgain(
      MongoClient mongo, KafkaClient kafka) {
    var playerId = createPlayer();

    completeStage(playerId);

    assertStageWasProjectedOnce(mongo, playerId);

    var stageCompletedRecord = stageCompletedRecord(kafka);
    var stageCompletedEventId = headerValue(stageCompletedRecord, "eventId");

    republish(stageCompletedRecord, kafka);

    assertDuplicateEventWasIgnored(mongo, playerId, stageCompletedEventId);
  }

  private void assertStageWasProjectedOnce(MongoClient mongo, UUID playerId) {
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() -> assertProfileHasSingleCompletedStage(mongo, playerId));
  }

  private void assertDuplicateEventWasIgnored(
      MongoClient mongo, UUID playerId, String stageCompletedEventId) {
    await()
        .during(Duration.ofSeconds(2))
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              assertProfileHasSingleCompletedStage(mongo, playerId);
              assertThat(
                      mongo.countDocuments(
                          "processed_events", eq("_id", UUID.fromString(stageCompletedEventId))))
                  .isEqualTo(1);
            });
  }

  private UUID createPlayer() {
    var playerId =
        given()
            .baseUri(writeBaseUrl())
            .contentType(ContentType.JSON)
            .body("{\"nickname\":\"" + NICKNAME + "\"}")
            .when()
            .post("/players")
            .then()
            .statusCode(201)
            .extract()
            .path("playerId")
            .toString();

    return UUID.fromString(playerId);
  }

  private void completeStage(UUID playerId) {
    given()
        .baseUri(writeBaseUrl())
        .contentType(ContentType.JSON)
        .body("{\"xpGained\":" + XP_GAINED + "}")
        .when()
        .post("/players/{playerId}/stages/{stageCode}/completion", playerId.toString(), STAGE_CODE)
        .then()
        .statusCode(204);
  }

  private ConsumerRecord<String, String> stageCompletedRecord(KafkaClient kafka) {
    return kafka.consumeFromBeginning(Duration.ofSeconds(2)).stream()
        .filter(record -> "StageCompletedMessage".equals(headerValue(record, "eventType")))
        .findFirst()
        .orElseThrow(() -> new AssertionError("StageCompletedMessage was not published"));
  }

  private void republish(ConsumerRecord<String, String> record, KafkaClient kafka) {
    kafka.send(
        record.key(),
        record.value(),
        Map.of("eventId", headerValue(record, "eventId"), "eventType", headerValue(record, "eventType")));
  }

  private void assertProfileHasSingleCompletedStage(MongoClient mongo, UUID playerId) {
    var profiles = mongo.findAll("player_profiles");

    assertThat(profiles)
        .singleElement()
        .satisfies(
            profile -> {
              assertThat(documentIdAsString(profile.get("_id"))).isEqualTo(playerId.toString());
              assertThat(profile.getString("nickname")).isEqualTo(NICKNAME);
              assertThat(profile.getInteger("experience")).isEqualTo(XP_GAINED);

              var completedStages = profile.getList("completedStages", Document.class);
              assertThat(completedStages).singleElement().satisfies(this::assertCompletedStage);
            });
  }

  private void assertCompletedStage(Document stage) {
    assertThat(stage.getString("stageCode")).isEqualTo(STAGE_CODE);
    assertThat(stage.getInteger("xpGained")).isEqualTo(XP_GAINED);
  }

  private String headerValue(ConsumerRecord<String, String> record, String name) {
    var header = record.headers().lastHeader(name);
    assertThat(header).as("Kafka header " + name).isNotNull();
    return new String(header.value(), StandardCharsets.UTF_8);
  }

  private String documentIdAsString(Object id) {
    if (id instanceof UUID uuid) {
      return uuid.toString();
    }

    if (id instanceof Binary binary && binary.getData().length == 16) {
      var buffer = ByteBuffer.wrap(binary.getData());
      return new UUID(buffer.getLong(), buffer.getLong()).toString();
    }

    return id.toString();
  }

  private String writeBaseUrl() {
    return propertyOrEnvironment(
        "integration.write.base-url", "INTEGRATION_WRITE_BASE_URL", "http://localhost:8080");
  }

  private String propertyOrEnvironment(String propertyName, String environmentName, String fallback) {
    var propertyValue = System.getProperty(propertyName);
    if (propertyValue != null && !propertyValue.isBlank()) {
      return propertyValue;
    }

    var environmentValue = System.getenv(environmentName);
    if (environmentValue != null && !environmentValue.isBlank()) {
      return environmentValue;
    }

    return fallback;
  }
}
