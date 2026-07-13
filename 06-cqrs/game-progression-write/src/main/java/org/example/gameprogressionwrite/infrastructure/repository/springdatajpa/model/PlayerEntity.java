package org.example.gameprogressionwrite.infrastructure.repository.springdatajpa.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.example.gameprogressionwrite.domain.model.Player;

@Entity
@Table(name = "players")
public class PlayerEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "nickname", nullable = false, length = 100)
  private String nickname;

  @Column(name = "experience", nullable = false)
  private int experience;

  @Column(name = "level", nullable = false)
  private int level;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(
      mappedBy = "player",
      fetch = FetchType.EAGER,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private List<CompletedStageEntity> completedStages;

  protected PlayerEntity() {}

  public PlayerEntity(
      UUID id, String nickname, int experience, int level, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.nickname = nickname;
    this.experience = experience;
    this.level = level;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public PlayerEntity(Player player) {
    this.id = player.id().value();
    this.nickname = player.nickname().value();
    this.experience = player.experience();
    this.level = player.level();
    this.createdAt = player.createdAt();
    this.updatedAt = player.updatedAt();
    this.completedStages =
        player.completedStages().stream()
            .map(
                stage ->
                    new CompletedStageEntity(
                        this,
                        stage.stageCode().value(),
                        stage.xpGained(),
                        stage.completedAt()))
            .toList();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public int getExperience() {
    return experience;
  }

  public void setExperience(int experience) {
    this.experience = experience;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public List<CompletedStageEntity> getCompletedStages() {
    return completedStages;
  }

  public void setCompletedStages(List<CompletedStageEntity> completedStages) {
    this.completedStages = completedStages;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    PlayerEntity that = (PlayerEntity) o;
    return experience == that.experience
        && level == that.level
        && Objects.equals(id, that.id)
        && Objects.equals(nickname, that.nickname)
        && Objects.equals(createdAt, that.createdAt)
        && Objects.equals(updatedAt, that.updatedAt)
        && Objects.equals(completedStages, that.completedStages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, nickname, experience, level, createdAt, updatedAt, completedStages);
  }
}
