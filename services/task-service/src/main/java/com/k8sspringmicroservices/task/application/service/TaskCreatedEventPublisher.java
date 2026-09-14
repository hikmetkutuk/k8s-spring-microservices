package com.k8sspringmicroservices.task.application.service;

import com.k8sspringmicroservices.task.application.port.out.TaskOutboxPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class TaskCreatedEventPublisher {

  private final TaskOutboxPort outbox;

  TaskCreatedEventPublisher(TaskOutboxPort outbox) {
    this.outbox = outbox;
  }

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  void onTaskCreated(TaskCreatedApplicationEvent event) {
    outbox.append(event.getPayload());
  }
}
