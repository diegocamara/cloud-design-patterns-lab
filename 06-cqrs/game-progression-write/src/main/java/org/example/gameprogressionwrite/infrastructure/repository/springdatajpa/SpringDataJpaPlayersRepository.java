package org.example.gameprogressionwrite.infrastructure.repository.springdatajpa;

import java.util.UUID;
import org.example.gameprogressionwrite.infrastructure.repository.springdatajpa.model.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaPlayersRepository extends JpaRepository<PlayerEntity, UUID> {}
