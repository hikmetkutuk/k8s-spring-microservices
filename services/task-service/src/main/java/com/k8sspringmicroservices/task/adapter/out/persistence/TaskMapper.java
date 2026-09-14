package com.k8sspringmicroservices.task.adapter.out.persistence;

import com.k8sspringmicroservices.task.domain.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

  public Task toDomain(TaskEntity entity) {
    return new Task(
        entity.getId(),
        entity.getOwnerId(),
        entity.getCatalogItemId(),
        entity.getTitle(),
        entity.getDescription(),
        entity.getQuantity(),
        entity.getStatus(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public TaskEntity toEntity(Task task) {
    return new TaskEntity(
        task.id(),
        task.ownerId(),
        task.catalogItemId(),
        task.title(),
        task.description(),
        task.quantity(),
        task.status(),
        task.createdAt(),
        task.updatedAt());
  }
}
