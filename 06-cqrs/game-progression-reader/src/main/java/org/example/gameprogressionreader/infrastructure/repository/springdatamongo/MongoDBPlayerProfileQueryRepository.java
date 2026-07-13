package org.example.gameprogressionreader.infrastructure.repository.springdatamongo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.gameprogressionreader.application.port.PlayerProfileQueryRepository;
import org.example.gameprogressionreader.application.query.model.CompletedStageView;
import org.example.gameprogressionreader.application.query.model.PlayerProfileView;
import org.example.gameprogressionreader.application.query.model.PlayerRankingItem;
import org.example.gameprogressionreader.infrastructure.repository.springdatamongo.model.PlayerProfileDocument;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class MongoDBPlayerProfileQueryRepository implements PlayerProfileQueryRepository {

  private final SpringDataMongoPlayerProfilesRepository springDataMongoPlayerProfilesRepository;

  public MongoDBPlayerProfileQueryRepository(
      SpringDataMongoPlayerProfilesRepository springDataMongoPlayerProfilesRepository) {
    this.springDataMongoPlayerProfilesRepository = springDataMongoPlayerProfilesRepository;
  }

  @Override
  public Optional<PlayerProfileView> findById(UUID playerId) {
    return this.springDataMongoPlayerProfilesRepository.findById(playerId).map(this::toProfileView);
  }

  @Override
  public List<PlayerRankingItem> findRanking(int limit) {
    return this.springDataMongoPlayerProfilesRepository
        .findAllByOrderByExperienceDesc(PageRequest.of(0, limit))
        .stream()
        .map(this::toRankingItem)
        .toList();
  }

  private PlayerRankingItem toRankingItem(PlayerProfileDocument playerProfileDocument) {
    return new PlayerRankingItem(
        playerProfileDocument.getId(),
        playerProfileDocument.getNickname(),
        playerProfileDocument.getExperience(),
        playerProfileDocument.getLevel());
  }

  private PlayerProfileView toProfileView(PlayerProfileDocument playerProfileDocument) {
    var completedStages =
        playerProfileDocument.getCompletedStages().stream()
            .map(
                stage ->
                    new CompletedStageView(
                        stage.stageCode(), stage.xpGained(), stage.completedAt()))
            .toList();

    return new PlayerProfileView(
        playerProfileDocument.getId(),
        playerProfileDocument.getNickname(),
        playerProfileDocument.getExperience(),
        playerProfileDocument.getLevel(),
        completedStages,
        playerProfileDocument.getCreatedAt(),
        playerProfileDocument.getUpdatedAt());
  }
}
