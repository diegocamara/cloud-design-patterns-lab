package org.example.gameprogressionreader.infrastructure.repository.springdatamongo.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "player_profiles")
public class PlayerProfileDocument {

  @Id private UUID id;

  private String nickname;
  private int experience;
  private int level;
  private List<CompletedStageDocument> completedStages;
  private Instant createdAt;
  private Instant updatedAt;

  protected PlayerProfileDocument() {}

  public PlayerProfileDocument(
      UUID id, String nickname, int experience, int level, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.nickname = nickname;
    this.experience = experience;
    this.level = level;
    this.completedStages = new ArrayList<>();
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public void completeStage(
      String stageCode, int xpGained, int totalExperience, int level, Instant completedAt) {
    this.completedStages.add(new CompletedStageDocument(stageCode, xpGained, completedAt));

    this.experience = totalExperience;
    this.level = level;
    this.updatedAt = completedAt;
  }

  public UUID getId() {
    return id;
  }

  public String getNickname() {
    return nickname;
  }

  public int getExperience() {
    return experience;
  }

  public int getLevel() {
    return level;
  }

  public List<CompletedStageDocument> getCompletedStages() {
    return List.copyOf(completedStages);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
