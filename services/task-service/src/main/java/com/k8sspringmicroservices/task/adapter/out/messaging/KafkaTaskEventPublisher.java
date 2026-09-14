package com.k8sspringmicroservices.task.adapter.out.messaging;

import com.k8sspringmicroservices.common.event.KafkaTopics;
import com.k8sspringmicroservices.common.event.TaskCreatedEvent;
import com.k8sspringmicroservices.task.application.port.out.TaskEventPublisherPort;
import java.util.concurrent.TimeUnit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaTaskEventPublisher implements TaskEventPublisherPort {

  private final KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate;

  public KafkaTaskEventPublisher(KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  public void publishTaskCreated(TaskCreatedEvent event) {
    try {
      kafkaTemplate.send(KafkaTopics.TASK_CREATED, event.taskId(), event).get(10, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while publishing task event", ex);
    } catch (Exception ex) {
      throw new IllegalStateException("Kafka did not acknowledge task event", ex);
    }
  }
}
