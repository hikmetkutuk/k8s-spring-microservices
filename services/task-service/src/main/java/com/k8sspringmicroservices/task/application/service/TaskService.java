package com.k8sspringmicroservices.task.application.service;

import com.k8sspringmicroservices.common.event.TaskCreatedEvent;
import com.k8sspringmicroservices.common.exception.ResourceNotFoundException;
import com.k8sspringmicroservices.task.application.port.in.TaskUseCase;
import com.k8sspringmicroservices.task.application.port.out.CatalogItemPort;
import com.k8sspringmicroservices.task.application.port.out.TaskRepositoryPort;
import com.k8sspringmicroservices.task.domain.Task;
import com.k8sspringmicroservices.task.domain.TaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService implements TaskUseCase {

  private final TaskRepositoryPort repository;
  private final CatalogItemPort catalogItemPort;
  private final ApplicationEventPublisher eventPublisher;

  public TaskService(
      TaskRepositoryPort repository,
      CatalogItemPort catalogItemPort,
      ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.catalogItemPort = catalogItemPort;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public Task create(
      String ownerId, String catalogItemId, String title, String description, int quantity) {
    catalogItemPort.getItem(catalogItemId);

    Instant now = Instant.now();
    Task task =
        new Task(
            UUID.randomUUID().toString(),
            ownerId,
            catalogItemId,
            title,
            description,
            quantity,
            TaskStatus.PENDING,
            now,
            now);
    Task saved = repository.save(task);

    eventPublisher.publishEvent(
        new TaskCreatedApplicationEvent(
            this,
            new TaskCreatedEvent(
                saved.id(),
                saved.ownerId(),
                saved.catalogItemId(),
                saved.title(),
                saved.quantity(),
                saved.createdAt())));

    return saved;
  }

  @Override
  public Task get(String id) {
    return findExistingOrThrow(id);
  }

  @Override
  public List<Task> listByOwner(String ownerId) {
    return repository.findAllByOwnerId(ownerId);
  }

  @Override
  @Transactional
  public Task update(String id, String title, String description, int quantity) {
    Task existing = findExistingOrThrow(id);
    Task updated =
        new Task(
            id,
            existing.ownerId(),
            existing.catalogItemId(),
            title,
            description,
            quantity,
            existing.status(),
            existing.createdAt(),
            Instant.now());
    return repository.save(updated);
  }

  @Override
  @Transactional
  public void delete(String id) {
    if (!repository.existsById(id)) {
      throw ResourceNotFoundException.forId("Task", id);
    }
    repository.deleteById(id);
  }

  private Task findExistingOrThrow(String id) {
    return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.forId("Task", id));
  }
}
