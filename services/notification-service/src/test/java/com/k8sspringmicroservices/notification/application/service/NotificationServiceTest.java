package com.k8sspringmicroservices.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.k8sspringmicroservices.common.event.TaskCreatedEvent;
import com.k8sspringmicroservices.notification.application.port.out.NotificationRepositoryPort;
import com.k8sspringmicroservices.notification.domain.Notification;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private NotificationRepositoryPort repository;

  private NotificationService service;

  @BeforeEach
  void setUp() {
    service = new NotificationService(repository);
  }

  @Test
  void handleTaskCreated_savesNotification_whenNotAlreadyProcessed() {
    when(repository.existsByTaskId("t-1")).thenReturn(false);
    TaskCreatedEvent event =
        new TaskCreatedEvent("t-1", "owner-1", "c-1", "Buy widget", 2, Instant.now());

    service.handleTaskCreated(event);

    verify(repository).save(any(Notification.class));
  }

  @Test
  void handleTaskCreated_skipsSave_whenAlreadyProcessed() {
    when(repository.existsByTaskId("t-1")).thenReturn(true);
    TaskCreatedEvent event =
        new TaskCreatedEvent("t-1", "owner-1", "c-1", "Buy widget", 2, Instant.now());

    service.handleTaskCreated(event);

    verify(repository, never()).save(any());
  }

  @Test
  void handleTaskCreated_swallowsDataIntegrityViolation_onConcurrentDuplicate() {
    when(repository.existsByTaskId("t-1")).thenReturn(false);
    when(repository.save(any(Notification.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate task_id"));
    TaskCreatedEvent event =
        new TaskCreatedEvent("t-1", "owner-1", "c-1", "Buy widget", 2, Instant.now());

    service.handleTaskCreated(event);
  }

  @Test
  void listByOwner_delegatesToRepository() {
    Notification notification = new Notification("n-1", "t-1", "owner-1", "message", Instant.now());
    when(repository.findAllByOwnerId("owner-1")).thenReturn(List.of(notification));

    assertThat(service.listByOwner("owner-1")).containsExactly(notification);
  }
}
