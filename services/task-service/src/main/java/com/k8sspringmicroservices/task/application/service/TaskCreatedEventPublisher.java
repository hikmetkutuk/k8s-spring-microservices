package com.k8sspringmicroservices.task.application.service;

import com.k8sspringmicroservices.task.application.port.out.TaskEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class TaskCreatedEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(TaskCreatedEventPublisher.class);

  private final TaskEventPublisherPort eventPublisherPort;

  TaskCreatedEventPublisher(TaskEventPublisherPort eventPublisherPort) {
    this.eventPublisherPort = eventPublisherPort;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onTaskCreated(TaskCreatedApplicationEvent event) {
    try {
      eventPublisherPort.publishTaskCreated(event.getPayload());
    } catch (Exception ex) {
      log.error(
          "Failed to publish TaskCreatedEvent for taskId={}", event.getPayload().taskId(), ex);
    }
  }
}
