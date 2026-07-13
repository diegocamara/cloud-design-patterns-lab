package org.example.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.http.ContentType;
import java.time.Duration;
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

@DisplayName("Create player with invalid nickname")
@ExtendWith({
  DockerComposeEnvironmentExtension.class,
  PostgresExtension.class,
  MongoExtension.class,
  KafkaExtension.class
})
class CreatePlayerWithInvalidNicknameIntegrationTest {

  @Test
  @DisplayName("rejects blank nickname without creating state or publishing events")
  void shouldRejectBlankNicknameWithoutCreatingStateOrPublishingEvents(
      PostgresClient postgres, MongoClient mongo, KafkaClient kafka) {
    assertBlankNicknameIsRejected();

    assertWriteModelWasNotChanged(postgres);
    assertReadModelWasNotChanged(mongo);
    assertNoPlayerCreatedEventWasPublished(kafka);
  }

  private void assertBlankNicknameIsRejected() {
    given()
        .baseUri(writeBaseUrl())
        .contentType(ContentType.JSON)
        .body("{\"nickname\":\"\"}")
        .when()
        .post("/players")
        .then()
        .statusCode(400);
  }

  private void assertWriteModelWasNotChanged(PostgresClient postgres) {
    assertThat(postgres.countRows("players")).isZero();
    assertThat(postgres.countRows("completed_stages")).isZero();
    assertThat(postgres.countRows("outbox_events")).isZero();
  }

  private void assertReadModelWasNotChanged(MongoClient mongo) {
    assertThat(mongo.countDocuments("player_profiles")).isZero();
    assertThat(mongo.countDocuments("processed_events")).isZero();
  }

  private void assertNoPlayerCreatedEventWasPublished(KafkaClient kafka) {
    assertThat(kafka.consumeFromBeginning(Duration.ofSeconds(1)))
        .noneSatisfy(
            record -> {
              var eventTypeHeader = record.headers().lastHeader("eventType");
              assertThat(eventTypeHeader).isNotNull();
              assertThat(new String(eventTypeHeader.value())).isNotEqualTo("PlayerCreatedMessage");
            });
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
