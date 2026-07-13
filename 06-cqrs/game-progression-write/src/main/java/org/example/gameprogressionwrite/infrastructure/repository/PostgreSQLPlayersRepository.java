package org.example.gameprogressionwrite.infrastructure.repository;

import java.util.Optional;
import org.example.gameprogressionwrite.application.port.PlayersRepository;
import org.example.gameprogressionwrite.domain.model.*;
import org.example.gameprogressionwrite.infrastructure.repository.springdatajpa.SpringDataJpaPlayersRepository;
import org.example.gameprogressionwrite.infrastructure.repository.springdatajpa.model.PlayerEntity;
import org.springframework.stereotype.Repository;

@Repository
public class PostgreSQLPlayersRepository implements PlayersRepository {

  private final SpringDataJpaPlayersRepository springDataJpaPlayersRepository;

  public PostgreSQLPlayersRepository(
      SpringDataJpaPlayersRepository springDataJpaPlayersRepository) {
    this.springDataJpaPlayersRepository = springDataJpaPlayersRepository;
  }

  @Override
  public void save(Player player) {
    this.springDataJpaPlayersRepository.save(new PlayerEntity(player));
  }

  @Override
  public Optional<Player> findById(PlayerId playerId) {
    return this.springDataJpaPlayersRepository.findById(playerId.value()).map(this::toDomain);
  }

  private Player toDomain(PlayerEntity entity) {
    var completedStages =
        entity.getCompletedStages().stream()
            .map(
                stageEntity ->
                    new CompletedStage(
                        new StageCode(stageEntity.getId().getStageCode()),
                        stageEntity.getXpGained(),
                        stageEntity.getCompletedAt()))
            .toList();

    return Player.restore(
        new PlayerId(entity.getId()),
        new Nickname(entity.getNickname()),
        entity.getExperience(),
        entity.getLevel(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        completedStages);
  }
}
