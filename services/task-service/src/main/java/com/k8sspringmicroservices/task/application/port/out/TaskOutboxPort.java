package com.k8sspringmicroservices.task.application.port.out;

import com.k8sspringmicroservices.common.event.TaskCreatedEvent;

public interface TaskOutboxPort {
  void append(TaskCreatedEvent event);
}
