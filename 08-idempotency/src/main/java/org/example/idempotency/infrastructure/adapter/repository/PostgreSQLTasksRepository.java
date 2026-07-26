package org.example.idempotency.infrastructure.adapter.repository;

import org.example.idempotency.application.task.port.TasksRepository;
import org.example.idempotency.domain.task.model.Task;
import org.example.idempotency.infrastructure.adapter.repository.springdata.SpringDataJpaTasksRepository;
import org.example.idempotency.infrastructure.adapter.repository.springdata.model.TaskEntity;
import org.springframework.stereotype.Repository;

@Repository
public class PostgreSQLTasksRepository implements TasksRepository {

  private final SpringDataJpaTasksRepository springDataJpaTasksRepository;

  public PostgreSQLTasksRepository(SpringDataJpaTasksRepository springDataJpaTasksRepository) {
    this.springDataJpaTasksRepository = springDataJpaTasksRepository;
  }

  @Override
  public void save(Task task) {
    this.springDataJpaTasksRepository.save(new TaskEntity(task));
  }
}
