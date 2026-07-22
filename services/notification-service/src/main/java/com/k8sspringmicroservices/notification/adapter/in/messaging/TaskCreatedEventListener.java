package com.k8sspringmicroservices.notification.adapter.in.messaging;

import com.k8sspringmicroservices.common.event.KafkaTopics;
import com.k8sspringmicroservices.common.event.TaskCreatedEvent;
import com.k8sspringmicroservices.notification.application.port.in.NotificationUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TaskCreatedEventListener {

  private final NotificationUseCase notificationUseCase;

  public TaskCreatedEventListener(NotificationUseCase notificationUseCase) {
    this.notificationUseCase = notificationUseCase;
  }

  @KafkaListener(topics = KafkaTopics.TASK_CREATED, groupId = "${spring.kafka.consumer.group-id}")
  public void onTaskCreated(TaskCreatedEvent event) {
    notificationUseCase.handleTaskCreated(event);
  }
}
