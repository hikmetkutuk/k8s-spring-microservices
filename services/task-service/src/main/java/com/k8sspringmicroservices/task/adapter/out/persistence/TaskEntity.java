package com.k8sspringmicroservices.task.adapter.out.persistence;

import com.k8sspringmicroservices.task.domain.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tasks")
public class TaskEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private String id;

  @Column(name = "owner_id", nullable = false, updatable = false)
  private String ownerId;

  @Column(name = "catalog_item_id", nullable = false, updatable = false)
  private String catalogItemId;

  @Column(nullable = false)
  private String title;

  @Column private String description;

  @Column(nullable = false)
  private int quantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected TaskEntity() {}

  public TaskEntity(
      String id,
      String ownerId,
      String catalogItemId,
      String title,
      String description,
      int quantity,
      TaskStatus status,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.ownerId = ownerId;
    this.catalogItemId = catalogItemId;
    this.title = title;
    this.description = description;
    this.quantity = quantity;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getId() {
    return id;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public String getCatalogItemId() {
    return catalogItemId;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public int getQuantity() {
    return quantity;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
