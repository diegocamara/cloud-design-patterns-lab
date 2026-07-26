package org.example.idempotency.application.task.port;

import org.example.idempotency.domain.task.model.Task;

public interface TasksRepository {
  void save(Task task);
}
