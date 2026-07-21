package com.k8sspringmicroservices.task.adapter.in.web.dto;

import com.k8sspringmicroservices.task.domain.Task;
import com.k8sspringmicroservices.task.domain.TaskStatus;
import java.time.Instant;

public record TaskResponse(
    String id,
    String ownerId,
    String catalogItemId,
    String title,
    String description,
    int quantity,
    TaskStatus status,
    Instant createdAt,
    Instant updatedAt) {

  public static TaskResponse from(Task task) {
    return new TaskResponse(
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
