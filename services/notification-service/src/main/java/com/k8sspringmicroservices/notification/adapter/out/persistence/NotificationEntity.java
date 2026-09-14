package com.k8sspringmicroservices.notification.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(columnNames = "task_id"))
public class NotificationEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private String id;

  @Column(name = "task_id", nullable = false, updatable = false)
  private String taskId;

  @Column(name = "owner_id", nullable = false, updatable = false)
  private String ownerId;

  @Column(nullable = false)
  private String message;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected NotificationEntity() {}

  public NotificationEntity(
      String id, String taskId, String ownerId, String message, Instant createdAt) {
    this.id = id;
    this.taskId = taskId;
    this.ownerId = ownerId;
    this.message = message;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public String getTaskId() {
    return taskId;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public String getMessage() {
    return message;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
