package org.example.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.restassured.http.ContentType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.example.integration.environment.DockerComposeEnvironmentExtension;
import org.example.integration.kafka.KafkaExtension;
import org.example.integration.mongo.MongoClient;
import org.example.integration.mongo.MongoExtension;
import org.example.integration.postgres.PostgresExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Player ranking limit")
@ExtendWith({
  DockerComposeEnvironmentExtension.class,
  PostgresExtension.class,
  MongoExtension.class,
  KafkaExtension.class
})
class PlayerRankingLimitIntegrationTest {

  private static final String STAGE_CODE = "ranking-limit-stage";

  @Test
  @DisplayName("returns only the requested number of players ordered by experience")
  void shouldLimitRankingResultsKeepingExperienceOrder(MongoClient mongo) {
    var alice = createPlayer("alice");
    var bob = createPlayer("bob");
    var carol = createPlayer("carol");

    completeStage(alice, 500);
    completeStage(bob, 1_500);
    completeStage(carol, 1_000);

    assertAllProfilesWereProjectedToMongo(mongo);
    assertRankingLimitReturnsTopTwoPlayers();
  }

  private void assertAllProfilesWereProjectedToMongo(MongoClient mongo) {
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() -> assertThat(mongo.countDocuments("player_profiles")).isEqualTo(3));
  }

  private void assertRankingLimitReturnsTopTwoPlayers() {
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              var ranking = getPlayersRanking(2);

              assertThat(ranking).hasSize(2);
              assertThat(ranking)
                  .extracting(document -> document.getString("nickname"))
                  .containsExactly("bob", "carol")
                  .doesNotContain("alice");

              assertRankingItem(ranking.get(0), "bob", 1_500, 2);
              assertRankingItem(ranking.get(1), "carol", 1_000, 2);
            });
  }

  private UUID createPlayer(String nickname) {
    var playerId =
        given()
            .baseUri(writeBaseUrl())
            .contentType(ContentType.JSON)
            .body("{\"nickname\":\"" + nickname + "\"}")
            .when()
            .post("/players")
            .then()
            .statusCode(201)
            .extract()
            .path("playerId")
            .toString();

    return UUID.fromString(playerId);
  }

  private void completeStage(UUID playerId, int xpGained) {
    given()
        .baseUri(writeBaseUrl())
        .contentType(ContentType.JSON)
        .body("{\"xpGained\":" + xpGained + "}")
        .when()
        .post("/players/{playerId}/stages/{stageCode}/completion", playerId.toString(), STAGE_CODE)
        .then()
        .statusCode(204);
  }

  private List<Document> getPlayersRanking(int limit) {
    try {
      var request =
          HttpRequest.newBuilder()
              .uri(URI.create(readerBaseUrl() + "/ranking/players?limit=" + limit))
              .GET()
              .build();

      var response =
          HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

      assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
      return Document.parse("{\"ranking\":" + response.body() + "}")
          .getList("ranking", Document.class);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("HTTP request was interrupted", exception);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Could not request players ranking", exception);
    }
  }

  private void assertRankingItem(Document item, String nickname, int experience, int level) {
    assertThat(item.getString("playerId")).isNotBlank();
    assertThat(item.getString("nickname")).isEqualTo(nickname);
    assertThat(item.getInteger("experience")).isEqualTo(experience);
    assertThat(item.getInteger("level")).isEqualTo(level);
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
