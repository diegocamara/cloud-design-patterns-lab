package org.example.gameprogressionwrite.domain.model;

import java.time.Instant;
import java.util.*;
import org.example.gameprogressionwrite.domain.event.DomainEvent;
import org.example.gameprogressionwrite.domain.event.PlayerCreated;
import org.example.gameprogressionwrite.domain.event.StageCompleted;
import org.example.gameprogressionwrite.domain.exception.DomainException;

public final class Player {

  private static final int INITIAL_EXPERIENCE = 0;
  private static final int INITIAL_LEVEL = 1;
  private static final int XP_PER_LEVEL = 1000;

  private final PlayerId id;
  private final Map<StageCode, CompletedStage> completedStages;
  private List<DomainEvent> events;

  private Nickname nickname;
  private int experience;
  private int level;
  private final Instant createdAt;
  private Instant updatedAt;

  private Player(
      PlayerId id,
      Nickname nickname,
      int experience,
      int level,
      Instant createdAt,
      Instant updatedAt,
      Collection<CompletedStage> completedStages) {
    this.id = id;
    this.nickname = nickname;
    this.experience = experience;
    this.level = level;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.completedStages = new LinkedHashMap<>();
    this.events = new ArrayList<>();

    for (CompletedStage completedStage : completedStages) {
      this.completedStages.put(completedStage.stageCode(), completedStage);
    }
  }

  public static Player create(PlayerId id, Nickname nickname, Instant occurredAt) {
    Player player =
        new Player(
            id, nickname, INITIAL_EXPERIENCE, INITIAL_LEVEL, occurredAt, occurredAt, List.of());

    player.events.add(
        new PlayerCreated(UUID.randomUUID(), player.id, player.nickname.value(), occurredAt));

    return player;
  }

  public static Player restore(
      PlayerId id,
      Nickname nickname,
      int experience,
      int level,
      Instant createdAt,
      Instant updatedAt,
      Collection<CompletedStage> completedStages) {
    return new Player(id, nickname, experience, level, createdAt, updatedAt, completedStages);
  }

  public void completeStage(StageCode stageCode, int xpGained, Instant completedAt) {
    if (completedStages.containsKey(stageCode)) {
      throw new DomainException("Stage already completed by this player");
    }

    CompletedStage completedStage = new CompletedStage(stageCode, xpGained, completedAt);

    this.completedStages.put(stageCode, completedStage);
    this.experience += xpGained;
    this.level = calculateLevel(this.experience);
    this.updatedAt = completedAt;

    this.events.add(
        new StageCompleted(
            UUID.randomUUID(),
            this.id,
            stageCode.value(),
            xpGained,
            this.experience,
            this.level,
            completedAt));
  }

  public List<DomainEvent> pullEvents() {
    List<DomainEvent> pulledEvents = List.copyOf(this.events);
    this.events.clear();
    return pulledEvents;
  }

  public PlayerId id() {
    return id;
  }

  public Nickname nickname() {
    return nickname;
  }

  public int experience() {
    return experience;
  }

  public int level() {
    return level;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public List<CompletedStage> completedStages() {
    return List.copyOf(completedStages.values());
  }

  private static int calculateLevel(int experience) {
    return (experience / XP_PER_LEVEL) + INITIAL_LEVEL;
  }
}
