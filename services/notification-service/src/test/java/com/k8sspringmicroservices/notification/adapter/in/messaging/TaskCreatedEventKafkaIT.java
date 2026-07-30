package com.k8sspringmicroservices.notification.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.k8sspringmicroservices.common.event.KafkaTopics;
import com.k8sspringmicroservices.common.event.TaskCreatedEvent;
import com.k8sspringmicroservices.notification.application.port.in.NotificationUseCase;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

/**
 * task-service'in yayınladığı TaskCreatedEvent'in gerçek bir Kafka broker üzerinden
 * notification-service tarafındaki dinleyici tarafından doğru şekilde tüketilip
 * NotificationUseCase'e iletildiğini doğrular.
 */
@Testcontainers
class TaskCreatedEventKafkaIT {

  @Container
  static final ConfluentKafkaContainer KAFKA =
      new ConfluentKafkaContainer("confluentinc/cp-kafka:7.7.1");

  private KafkaTemplate<String, TaskCreatedEvent> producerTemplate;
  private KafkaMessageListenerContainer<String, TaskCreatedEvent> listenerContainer;

  @BeforeEach
  void setUp() {
    Map<String, Object> producerProps =
        Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    producerTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));

    Map<String, Object> consumerProps =
        Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            KAFKA.getBootstrapServers(),
            ConsumerConfig.GROUP_ID_CONFIG,
            "notification-service-test",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            JsonDeserializer.class,
            JsonDeserializer.TRUSTED_PACKAGES,
            "com.k8sspringmicroservices.common.event",
            JsonDeserializer.VALUE_DEFAULT_TYPE,
            TaskCreatedEvent.class.getName(),
            JsonDeserializer.USE_TYPE_INFO_HEADERS,
            false);

    DefaultKafkaConsumerFactory<String, TaskCreatedEvent> consumerFactory =
        new DefaultKafkaConsumerFactory<>(consumerProps);
    ContainerProperties containerProperties = new ContainerProperties(KafkaTopics.TASK_CREATED);
    listenerContainer = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
  }

  @AfterEach
  void tearDown() {
    if (listenerContainer.isRunning()) {
      listenerContainer.stop();
    }
    producerTemplate.destroy();
  }

  @Test
  void publishedTaskCreatedEvent_isConsumedAndForwardedToNotificationUseCase() {
    NotificationUseCase notificationUseCase = mock(NotificationUseCase.class);
    AtomicReference<TaskCreatedEvent> received = new AtomicReference<>();

    MessageListener<String, TaskCreatedEvent> messageListener =
        record -> {
          TaskCreatedEvent event = record.value();
          received.set(event);
          notificationUseCase.handleTaskCreated(event);
        };
    listenerContainer.setupMessageListener(messageListener);
    listenerContainer.start();

    TaskCreatedEvent event =
        new TaskCreatedEvent("t-1", "owner-1", "c-1", "Buy widget", 3, Instant.now());
    producerTemplate.send(KafkaTopics.TASK_CREATED, event.taskId(), event);

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() -> assertThat(received.get()).isNotNull());

    assertThat(received.get()).isEqualTo(event);
    verify(notificationUseCase).handleTaskCreated(event);
  }
}
