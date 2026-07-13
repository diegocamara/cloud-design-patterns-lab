package org.example.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.restassured.http.ContentType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;
import org.bson.types.Binary;
import org.example.integration.environment.DockerComposeEnvironmentExtension;
import org.example.integration.kafka.KafkaClient;
import org.example.integration.kafka.KafkaExtension;
import org.example.integration.mongo.MongoClient;
import org.example.integration.mongo.MongoExtension;
import org.example.integration.postgres.PostgresClient;
import org.example.integration.postgres.PostgresExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Complete stage with invalid XP")
@ExtendWith({
  DockerComposeEnvironmentExtension.class,
  PostgresExtension.class,
  MongoExtension.class,
  KafkaExtension.class
})
class CompleteStageWithInvalidXpIntegrationTest {

  private static final String NICKNAME = "invalid-xp-player";
  private static final String STAGE_CODE = "invalid-xp-stage";
  private static final int INVALID_XP_GAINED = 0;

  @Test
  @DisplayName("rejects non-positive XP without completing the stage or publishing an event")
  void shouldRejectNonPositiveXpWithoutCompletingStageOrPublishingEvent(
      PostgresClient postgres, MongoClient mongo, KafkaClient kafka) {
    var playerId = createPlayer();

    assertPlayerWasCreatedAndProjected(postgres, mongo, playerId);

    assertInvalidXpIsRejected(playerId);

    assertWriteModelWasNotChangedByRejectedCommand(postgres, playerId);
    assertReadModelWasNotChangedByRejectedCommand(mongo, playerId);
    assertNoStageCompletedEventWasPublished(kafka);
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

  private void assertPlayerWasCreatedAndProjected(
      PostgresClient postgres, MongoClient mongo, UUID playerId) {
    assertThat(postgres.countRows("players")).isEqualTo(1);
    assertThat(countOutboxEvents(postgres, "PlayerCreatedMessage")).isEqualTo(1);

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() -> assertProfileHasNoCompletedStages(mongo, playerId));
  }

  private void assertInvalidXpIsRejected(UUID playerId) {
    given()
        .baseUri(writeBaseUrl())
        .contentType(ContentType.JSON)
        .body("{\"xpGained\":" + INVALID_XP_GAINED + "}")
        .when()
        .post("/players/{playerId}/stages/{stageCode}/completion", playerId.toString(), STAGE_CODE)
        .then()
        .statusCode(400);
  }

  private void assertWriteModelWasNotChangedByRejectedCommand(
      PostgresClient postgres, UUID playerId) {
    await()
        .during(Duration.ofSeconds(2))
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              assertThat(playerRows(postgres, playerId))
                  .singleElement()
                  .satisfies(
                      row -> {
                        assertThat(row.get("experience")).isEqualTo(0);
                        assertThat(row.get("level")).isEqualTo(1);
                      });

              assertThat(postgres.countRows("completed_stages")).isZero();
              assertThat(countOutboxEvents(postgres, "PlayerCreatedMessage")).isEqualTo(1);
              assertThat(countOutboxEvents(postgres, "StageCompletedMessage")).isZero();
            });
  }

  private void assertReadModelWasNotChangedByRejectedCommand(
      MongoClient mongo, UUID playerId) {
    await()
        .during(Duration.ofSeconds(2))
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertProfileHasNoCompletedStages(mongo, playerId));
  }

  private void assertNoStageCompletedEventWasPublished(KafkaClient kafka) {
    assertThat(kafka.consumeFromBeginning(Duration.ofSeconds(1)))
        .noneSatisfy(
            record -> {
              var eventTypeHeader = record.headers().lastHeader("eventType");
              assertThat(eventTypeHeader).isNotNull();
              assertThat(new String(eventTypeHeader.value(), StandardCharsets.UTF_8))
                  .isEqualTo("StageCompletedMessage");
            });
  }

  private void assertProfileHasNoCompletedStages(MongoClient mongo, UUID playerId) {
    var profiles = mongo.findAll("player_profiles");

    assertThat(profiles)
        .singleElement()
        .satisfies(
            profile -> {
              assertThat(documentIdAsString(profile.get("_id"))).isEqualTo(playerId.toString());
              assertThat(profile.getString("nickname")).isEqualTo(NICKNAME);
              assertThat(profile.getInteger("experience")).isZero();
              assertThat(profile.getInteger("level")).isEqualTo(1);
              assertThat(profile.getList("completedStages", Document.class)).isEmpty();
            });
  }

  private java.util.List<Map<String, Object>> playerRows(PostgresClient postgres, UUID playerId) {
    return postgres.queryForList(
        "select id, nickname, experience, level from players where id = ?", playerId);
  }

  private long countOutboxEvents(PostgresClient postgres, String eventType) {
    return postgres
        .queryForList("select count(*) as total from outbox_events where event_type = ?", eventType)
        .stream()
        .findFirst()
        .map(row -> ((Number) row.get("total")).longValue())
        .orElseThrow();
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
