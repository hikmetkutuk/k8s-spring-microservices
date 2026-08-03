package com.k8sspringmicroservices.notification.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.k8sspringmicroservices.common.event.TaskCreatedEvent;
import com.k8sspringmicroservices.notification.application.port.in.NotificationUseCase;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskCreatedEventListenerTest {

  @Mock private NotificationUseCase notificationUseCase;

  @InjectMocks private TaskCreatedEventListener eventListener;

  @Test
  void onTaskCreated_delegatesToNotificationUseCase() {
    TaskCreatedEvent event =
        new TaskCreatedEvent(
            "task-uuid-1", "owner-uuid-1", "catalog-item-uuid-1", "Buy Gadgets", 5, Instant.now());

    eventListener.onTaskCreated(event);

    verify(notificationUseCase).handleTaskCreated(event);
  }

  @Test
  void onTaskCreated_passesCorrectEventFields() {
    Instant now = Instant.now();
    TaskCreatedEvent event =
        new TaskCreatedEvent(
            "task-uuid-2", "owner-uuid-2", "catalog-item-uuid-2", "Complete Report", 3, now);

    eventListener.onTaskCreated(event);

    ArgumentCaptor<TaskCreatedEvent> captor = ArgumentCaptor.forClass(TaskCreatedEvent.class);
    verify(notificationUseCase).handleTaskCreated(captor.capture());

    TaskCreatedEvent captured = captor.getValue();
    assertThat(captured.taskId()).isEqualTo("task-uuid-2");
    assertThat(captured.ownerId()).isEqualTo("owner-uuid-2");
    assertThat(captured.catalogItemId()).isEqualTo("catalog-item-uuid-2");
    assertThat(captured.title()).isEqualTo("Complete Report");
    assertThat(captured.quantity()).isEqualTo(3);
    assertThat(captured.createdAt()).isEqualTo(now);
  }
}
