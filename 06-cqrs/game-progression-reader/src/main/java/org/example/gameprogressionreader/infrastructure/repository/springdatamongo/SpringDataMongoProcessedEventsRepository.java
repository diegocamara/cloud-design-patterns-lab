package org.example.gameprogressionreader.infrastructure.repository.springdatamongo;

import java.util.UUID;
import org.example.gameprogressionreader.infrastructure.repository.springdatamongo.model.ProcessedEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataMongoProcessedEventsRepository
    extends MongoRepository<ProcessedEventDocument, UUID> {}
