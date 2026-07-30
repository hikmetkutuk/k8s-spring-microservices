package com.k8sspringmicroservices.task.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.k8sspringmicroservices.common.event.TaskCreatedEvent;
import com.k8sspringmicroservices.common.exception.ResourceNotFoundException;
import com.k8sspringmicroservices.task.application.port.out.CatalogItemPort;
import com.k8sspringmicroservices.task.application.port.out.TaskEventPublisherPort;
import com.k8sspringmicroservices.task.application.port.out.TaskRepositoryPort;
import com.k8sspringmicroservices.task.domain.CatalogItemSummary;
import com.k8sspringmicroservices.task.domain.Task;
import com.k8sspringmicroservices.task.domain.TaskStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock private TaskRepositoryPort repository;
  @Mock private CatalogItemPort catalogItemPort;
  @Mock private TaskEventPublisherPort eventPublisherPort;

  private TaskService service;

  @BeforeEach
  void setUp() {
    service = new TaskService(repository, catalogItemPort, eventPublisherPort);
  }

  @Test
  void create_validatesCatalogItem_savesTask_andPublishesEvent() {
    when(catalogItemPort.getItem("c-1"))
        .thenReturn(new CatalogItemSummary("c-1", "Widget", BigDecimal.TEN, 5));
    when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Task result = service.create("owner-1", "c-1", "Buy widget", "desc", 2);

    assertThat(result.ownerId()).isEqualTo("owner-1");
    assertThat(result.catalogItemId()).isEqualTo("c-1");
    assertThat(result.status()).isEqualTo(TaskStatus.PENDING);

    ArgumentCaptor<TaskCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCreatedEvent.class);
    verify(eventPublisherPort).publishTaskCreated(eventCaptor.capture());
    TaskCreatedEvent published = eventCaptor.getValue();
    assertThat(published.taskId()).isEqualTo(result.id());
    assertThat(published.ownerId()).isEqualTo("owner-1");
    assertThat(published.catalogItemId()).isEqualTo("c-1");
    assertThat(published.quantity()).isEqualTo(2);
  }

  @Test
  void create_doesNotSaveOrPublish_whenCatalogItemDoesNotExist() {
    when(catalogItemPort.getItem("missing"))
        .thenThrow(ResourceNotFoundException.forId("CatalogItem", "missing"));

    assertThatThrownBy(() -> service.create("owner-1", "missing", "title", "desc", 1))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(repository, never()).save(any());
    verify(eventPublisherPort, never()).publishTaskCreated(any());
  }

  @Test
  void get_returnsTask_whenFound() {
    Task task = sampleTask("t-1");
    when(repository.findById("t-1")).thenReturn(Optional.of(task));

    assertThat(service.get("t-1")).isEqualTo(task);
  }

  @Test
  void get_throwsResourceNotFound_whenMissing() {
    when(repository.findById("t-1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get("t-1")).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void listByOwner_delegatesToRepository() {
    Task task = sampleTask("t-1");
    when(repository.findAllByOwnerId("owner-1")).thenReturn(List.of(task));

    assertThat(service.listByOwner("owner-1")).containsExactly(task);
  }

  @Test
  void update_preservesOwnerCatalogItemAndCreatedAt() {
    Task existing = sampleTask("t-1");
    when(repository.findById("t-1")).thenReturn(Optional.of(existing));
    when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Task updated = service.update("t-1", "New title", "new desc", 9);

    assertThat(updated.ownerId()).isEqualTo(existing.ownerId());
    assertThat(updated.catalogItemId()).isEqualTo(existing.catalogItemId());
    assertThat(updated.createdAt()).isEqualTo(existing.createdAt());
    assertThat(updated.title()).isEqualTo("New title");
    assertThat(updated.quantity()).isEqualTo(9);
  }

  @Test
  void delete_deletes_whenExists() {
    when(repository.existsById("t-1")).thenReturn(true);

    service.delete("t-1");

    verify(repository).deleteById("t-1");
  }

  @Test
  void delete_throwsResourceNotFound_whenMissing() {
    when(repository.existsById("t-1")).thenReturn(false);

    assertThatThrownBy(() -> service.delete("t-1")).isInstanceOf(ResourceNotFoundException.class);

    verify(repository, never()).deleteById(any());
  }

  private Task sampleTask(String id) {
    Instant now = Instant.now();
    return new Task(id, "owner-1", "c-1", "Buy widget", "desc", 2, TaskStatus.PENDING, now, now);
  }
}
