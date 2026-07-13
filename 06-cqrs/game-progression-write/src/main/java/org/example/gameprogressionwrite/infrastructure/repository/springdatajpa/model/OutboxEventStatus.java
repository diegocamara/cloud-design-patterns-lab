package org.example.gameprogressionwrite.infrastructure.repository.springdatajpa.model;

public enum OutboxEventStatus {
  PENDING,
  PUBLISHED,
  FAILED
}
