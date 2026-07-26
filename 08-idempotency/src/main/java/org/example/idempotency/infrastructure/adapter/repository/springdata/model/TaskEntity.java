package org.example.idempotency.infrastructure.adapter.repository.springdata.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.example.idempotency.domain.task.model.Task;

@Entity
@Table(name = "tasks")
public class TaskEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public TaskEntity() {}

  public TaskEntity(Task task) {
    this.id = task.getId().value();
    this.title = task.getTitle().value();
    this.createdAt = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    TaskEntity that = (TaskEntity) o;
    return Objects.equals(id, that.id)
        && Objects.equals(title, that.title)
        && Objects.equals(createdAt, that.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, createdAt);
  }
}
