package org.example.gameprogressionwrite.infrastructure.repository.springdatajpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "completed_stages")
public class CompletedStageEntity {

  @EmbeddedId private CompletedStageId id;

  @MapsId("playerId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false)
  private PlayerEntity player;

  @Column(name = "xp_gained", nullable = false)
  private int xpGained;

  @Column(name = "completed_at", nullable = false)
  private Instant completedAt;

  protected CompletedStageEntity() {}

  public CompletedStageEntity(
      PlayerEntity player, String stageCode, int xpGained, Instant completedAt) {
    this.player = player;
    this.id = new CompletedStageId(player.getId(), stageCode);
    this.xpGained = xpGained;
    this.completedAt = completedAt;
  }

  public CompletedStageId getId() {
    return id;
  }

  public void setId(CompletedStageId id) {
    this.id = id;
  }

  public PlayerEntity getPlayer() {
    return player;
  }

  public void setPlayer(PlayerEntity player) {
    this.player = player;
  }

  public int getXpGained() {
    return xpGained;
  }

  public void setXpGained(int xpGained) {
    this.xpGained = xpGained;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    CompletedStageEntity that = (CompletedStageEntity) o;
    return xpGained == that.xpGained
        && Objects.equals(id, that.id)
        && Objects.equals(player, that.player)
        && Objects.equals(completedAt, that.completedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, player, xpGained, completedAt);
  }
}
