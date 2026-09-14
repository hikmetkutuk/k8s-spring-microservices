package com.k8sspringmicroservices.task.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.k8sspringmicroservices.common.event.KafkaTopics;
import com.k8sspringmicroservices.common.event.TaskCreatedEvent;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaTaskEventPublisherTest {
  @Test
  @SuppressWarnings("unchecked")
  void asynchronousBrokerFailureIsPropagatedToRelay() {
    KafkaTemplate<String, TaskCreatedEvent> template = mock(KafkaTemplate.class);
    var event = new TaskCreatedEvent("task", "owner", "catalog", "title", 1, Instant.now());
    when(template.send(KafkaTopics.TASK_CREATED, event.taskId(), event))
        .thenReturn(
            CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));
    assertThatThrownBy(() -> new KafkaTaskEventPublisher(template).publishTaskCreated(event))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Kafka did not acknowledge task event");
  }
}
