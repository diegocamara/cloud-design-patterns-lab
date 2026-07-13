package org.example.gameprogressionreader.infrastructure.repository.springdatamongo;

import java.util.List;
import java.util.UUID;
import org.example.gameprogressionreader.infrastructure.repository.springdatamongo.model.PlayerProfileDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataMongoPlayerProfilesRepository
    extends MongoRepository<PlayerProfileDocument, UUID> {

  List<PlayerProfileDocument> findAllByOrderByExperienceDesc(Pageable pageable);
}
