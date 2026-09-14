package com.k8sspringmicroservices.task.application.port.out;

import com.k8sspringmicroservices.common.event.TaskCreatedEvent;

public interface TaskEventPublisherPort {
  void publishTaskCreated(TaskCreatedEvent event);
}
