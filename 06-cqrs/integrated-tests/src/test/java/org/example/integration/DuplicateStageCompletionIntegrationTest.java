package org.example.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.restassured.http.ContentType;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;
import org.bson.types.Binary;
import org.example.integration.environment.DockerComposeEnvironmentExtension;
import org.example.integration.kafka.KafkaExtension;
import org.example.integration.mongo.MongoClient;
import org.example.integration.mongo.MongoExtension;
import org.example.integration.postgres.PostgresClient;
import org.example.integration.postgres.PostgresExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Duplicate stage completion")
@ExtendWith({
  DockerComposeEnvironmentExtension.class,
  PostgresExtension.class,
  MongoExtension.class,
  KafkaExtension.class
})
class DuplicateStageCompletionIntegrationTest {

  private static final String NICKNAME = "duplicate-stage-player";
  private static final String STAGE_CODE = "duplicate-stage";
  private static final int XP_GAINED = 500;

  @Test
  @DisplayName("rejects completing the same stage twice without changing write or read models")
  void shouldRejectCompletingTheSameStageTwice(
      PostgresClient postgres, MongoClient mongo) {
    var playerId = createPlayer();

    completeStage(playerId);
    assertStageWasProjectedOnce(mongo, playerId);

    assertDuplicateStageCompletionIsRejected(playerId);

    assertWriteModelWasNotChangedByRejectedCommand(postgres, playerId);
    assertReadModelWasNotChangedByRejectedCommand(mongo, playerId);
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

  private void assertDuplicateStageCompletionIsRejected(UUID playerId) {
    given()
        .baseUri(writeBaseUrl())
        .contentType(ContentType.JSON)
        .body("{\"xpGained\":" + XP_GAINED + "}")
        .when()
        .post("/players/{playerId}/stages/{stageCode}/completion", playerId.toString(), STAGE_CODE)
        .then()
        .statusCode(409);
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
                        assertThat(row.get("experience")).isEqualTo(XP_GAINED);
                        assertThat(row.get("level")).isEqualTo(1);
                      });

              assertThat(postgres.countRows("completed_stages")).isEqualTo(1);
              assertThat(countOutboxEvents(postgres, "StageCompletedMessage")).isEqualTo(1);
            });
  }

  private void assertStageWasProjectedOnce(MongoClient mongo, UUID playerId) {
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() -> assertProfileHasSingleCompletedStage(mongo, playerId));
  }

  private void assertReadModelWasNotChangedByRejectedCommand(MongoClient mongo, UUID playerId) {
    await()
        .during(Duration.ofSeconds(2))
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertProfileHasSingleCompletedStage(mongo, playerId));
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
