package org.example.gameprogressionwrite.infrastructure.repository.springdatajpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CompletedStageId implements Serializable {

  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  @Column(name = "stage_code", nullable = false, length = 100)
  private String stageCode;

  protected CompletedStageId() {}

  public CompletedStageId(UUID playerId, String stageCode) {
    this.playerId = Objects.requireNonNull(playerId);
    this.stageCode = Objects.requireNonNull(stageCode);
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public void setPlayerId(UUID playerId) {
    this.playerId = playerId;
  }

  public String getStageCode() {
    return stageCode;
  }

  public void setStageCode(String stageCode) {
    this.stageCode = stageCode;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    CompletedStageId that = (CompletedStageId) o;
    return Objects.equals(playerId, that.playerId) && Objects.equals(stageCode, that.stageCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(playerId, stageCode);
  }
}
