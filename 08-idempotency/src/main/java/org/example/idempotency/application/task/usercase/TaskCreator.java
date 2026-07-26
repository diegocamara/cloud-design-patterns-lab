package org.example.idempotency.application.task.usercase;

import java.util.Objects;
import java.util.UUID;
import org.example.idempotency.application.task.model.TaskCreatorInput;
import org.example.idempotency.application.task.port.TasksRepository;
import org.example.idempotency.domain.task.model.Task;

public final class TaskCreator {
  private final TasksRepository tasksRepository;

  public TaskCreator(TasksRepository tasksRepository) {
    this.tasksRepository = tasksRepository;
  }

  public Task create(TaskCreatorInput input) {
    Objects.requireNonNull(input, "TaskCreatorInput must not be null");
    final var task = Task.of(UUID.randomUUID(), input.title());
    this.tasksRepository.save(task);
    return task;
  }
}
