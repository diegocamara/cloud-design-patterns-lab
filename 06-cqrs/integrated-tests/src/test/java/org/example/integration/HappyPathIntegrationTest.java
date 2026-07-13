package org.example.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.restassured.http.ContentType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

@DisplayName("Player progression happy path")
@ExtendWith({
  DockerComposeEnvironmentExtension.class,
  PostgresExtension.class,
  MongoExtension.class,
  KafkaExtension.class
})
class HappyPathIntegrationTest {

  private static final String NICKNAME = "player";
  private static final String STAGE_CODE = "stage-1";
  private static final int XP_GAINED = 1_250;
  private static final int EXPECTED_LEVEL = 2;

  @Test
  @DisplayName(
      "creates a player, completes a stage, publishes events, and exposes the updated profile")
  void shouldCreatePlayerCompleteStagePublishEventsAndExposeUpdatedProfile(
      PostgresClient postgres, MongoClient mongo, KafkaClient kafka) {
    var playerId = createPlayer();

    assertPlayerWasCreatedInWriteModel(postgres);

    completeStage(playerId);

    assertStageCompletionWasPersistedInWriteModel(postgres, playerId);
    assertUpdatedProfileIsAvailableThroughQueryApi(playerId);
    assertUpdatedProfileWasProjectedToMongo(mongo, playerId);
    assertDomainEventsWerePublishedToKafka(kafka);
  }

  private void assertPlayerWasCreatedInWriteModel(PostgresClient postgres) {
    assertThat(postgres.countRows("players")).isEqualTo(1);
    assertThat(countOutboxEvents(postgres, "PlayerCreatedMessage")).isEqualTo(1);
  }

  private void assertStageCompletionWasPersistedInWriteModel(
      PostgresClient postgres, UUID playerId) {
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              assertThat(playerRows(postgres, playerId))
                  .singleElement()
                  .satisfies(
                      row -> {
                        assertThat(row.get("experience")).isEqualTo(XP_GAINED);
                        assertThat(row.get("level")).isEqualTo(EXPECTED_LEVEL);
                      });

              assertThat(completedStageRows(postgres, playerId)).singleElement();
              assertThat(countOutboxEvents(postgres, "StageCompletedMessage")).isEqualTo(1);
            });
  }

  private void assertUpdatedProfileIsAvailableThroughQueryApi(UUID playerId) {
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              var profile = getPlayerProfile(playerId);

              assertThat(profile.getString("playerId")).isEqualTo(playerId.toString());
              assertThat(profile.getString("nickname")).isEqualTo(NICKNAME);
              assertThat(profile.getInteger("experience")).isEqualTo(XP_GAINED);
              assertThat(profile.getInteger("level")).isEqualTo(EXPECTED_LEVEL);

              var completedStages = profile.getList("completedStages", Document.class);
              assertThat(completedStages).singleElement().satisfies(this::assertCompletedStage);
            });
  }

  private void assertUpdatedProfileWasProjectedToMongo(MongoClient mongo, UUID playerId) {
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              var profiles = mongo.findAll("player_profiles");
              assertThat(profiles)
                  .singleElement()
                  .satisfies(profile -> assertProfile(playerId, profile));
            });
  }

  private void assertDomainEventsWerePublishedToKafka(KafkaClient kafka) {
    assertThat(kafka.consumeFromBeginning(Duration.ofSeconds(2)))
        .extracting(record -> record.headers().lastHeader("eventType"))
        .extracting(header -> new String(header.value(), StandardCharsets.UTF_8))
        .contains("PlayerCreatedMessage", "StageCompletedMessage");
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

  private Document getPlayerProfile(UUID playerId) {
    try {
      var request =
          HttpRequest.newBuilder()
              .uri(URI.create(readerBaseUrl() + "/players/" + playerId + "/profile"))
              .GET()
              .build();

      var response =
          HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

      assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
      return Document.parse(response.body());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("HTTP request was interrupted", exception);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Could not request player profile", exception);
    }
  }

  private long countOutboxEvents(PostgresClient postgres, String eventType) {
    return postgres
        .queryForList("select count(*) as total from outbox_events where event_type = ?", eventType)
        .stream()
        .findFirst()
        .map(row -> ((Number) row.get("total")).longValue())
        .orElseThrow();
  }

  private java.util.List<Map<String, Object>> playerRows(PostgresClient postgres, UUID playerId) {
    return postgres.queryForList(
        "select id, nickname, experience, level from players where id = ?", playerId);
  }

  private java.util.List<Map<String, Object>> completedStageRows(
      PostgresClient postgres, UUID playerId) {
    return postgres.queryForList(
        "select player_id, stage_code, xp_gained from completed_stages where player_id = ?",
        playerId);
  }

  private void assertProfile(UUID playerId, Document profile) {
    assertThat(documentIdAsString(profile.get("_id"))).isEqualTo(playerId.toString());
    assertThat(profile.getString("nickname")).isEqualTo(NICKNAME);
    assertThat(profile.getInteger("experience")).isEqualTo(XP_GAINED);
    assertThat(profile.getInteger("level")).isEqualTo(EXPECTED_LEVEL);

    var completedStages = profile.getList("completedStages", Document.class);
    assertThat(completedStages).singleElement().satisfies(this::assertCompletedStage);
  }

  private void assertCompletedStage(Document stage) {
    assertThat(stage.getString("stageCode")).isEqualTo(STAGE_CODE);
    assertThat(stage.getInteger("xpGained")).isEqualTo(XP_GAINED);
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

  private String readerBaseUrl() {
    return propertyOrEnvironment(
        "integration.reader.base-url", "INTEGRATION_READER_BASE_URL", "http://localhost:8081");
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
