package com.k8sspringmicroservices.task.application.service;

import com.k8sspringmicroservices.common.event.TaskCreatedEvent;
import org.springframework.context.ApplicationEvent;

class TaskCreatedApplicationEvent extends ApplicationEvent {

  private final TaskCreatedEvent payload;

  TaskCreatedApplicationEvent(Object source, TaskCreatedEvent payload) {
    super(source);
    this.payload = payload;
  }

  TaskCreatedEvent getPayload() {
    return payload;
  }
}
