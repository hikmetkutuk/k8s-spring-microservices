package com.k8sspringmicroservices.task.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.k8sspringmicroservices.common.event.TaskCreatedEvent;
import com.k8sspringmicroservices.task.adapter.out.messaging.TaskOutboxRelay;
import com.k8sspringmicroservices.task.adapter.out.persistence.*;
import com.k8sspringmicroservices.task.application.port.out.CatalogItemPort;
import com.k8sspringmicroservices.task.application.port.out.TaskEventPublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
  TaskService.class,
  TaskCreatedEventPublisher.class,
  TaskOutboxAdapter.class,
  TaskPersistenceAdapter.class,
  TaskMapper.class
})
class TaskOutboxIT {
  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired TaskService service;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager manager;
  @MockitoBean CatalogItemPort catalog;
  private TaskEventPublisherPort publisher;
  private io.micrometer.core.instrument.simple.SimpleMeterRegistry registry;

  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM outbox_events");
    jdbc.update("DELETE FROM tasks");
    publisher = mock(TaskEventPublisherPort.class);
    registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
    new TaskOutboxMetrics(jdbc).bindTo(registry);
  }

  @Test
  void commitStoresTaskAndEvent_thenRelayDeletesAcknowledgedEvent() {
    var task = service.create("owner", "catalog", "title", "description", 2);
    assertThat(count("tasks")).isEqualTo(1);
    assertThat(count("outbox_events")).isEqualTo(1);
    relay().publishPending();
    var captor = org.mockito.ArgumentCaptor.forClass(TaskCreatedEvent.class);
    verify(publisher).publishTaskCreated(captor.capture());
    assertThat(captor.getValue().taskId()).isEqualTo(task.id());
    assertThat(captor.getValue().title()).isEqualTo(task.title());
    assertThat(count("outbox_events")).isZero();
    assertThat(count("tasks")).isEqualTo(1);
  }

  @Test
  void rollbackLeavesNeitherTaskNorEvent() {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              service.create("owner", "catalog", "title", null, 1);
              status.setRollbackOnly();
            });
    assertThat(count("tasks")).isZero();
    assertThat(count("outbox_events")).isZero();
  }

  @Test
  void outboxInsertFailureRollsBackTask() {
    jdbc.execute("ALTER TABLE outbox_events ADD CONSTRAINT reject_test CHECK (quantity < 0)");
    try {
      assertThatThrownBy(() -> service.create("owner", "catalog", "title", null, 1))
          .isInstanceOf(RuntimeException.class);
      assertThat(count("tasks")).isZero();
      assertThat(count("outbox_events")).isZero();
    } finally {
      jdbc.execute("ALTER TABLE outbox_events DROP CONSTRAINT reject_test");
    }
  }

  @Test
  void failedPublishSurvivesRelayRecreationAndHonorsRetryDelay() {
    service.create("owner", "catalog", "title", null, 1);
    doThrow(new IllegalStateException("Kafka unavailable"))
        .when(publisher)
        .publishTaskCreated(any());
    relay().publishPending();
    assertThat(registry.get("task.outbox.publish.failures").counter().count()).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT attempts FROM outbox_events", Integer.class))
        .isEqualTo(1);
    relay().publishPending();
    verify(publisher, times(1)).publishTaskCreated(any());
    jdbc.update("UPDATE outbox_events SET next_attempt_at = CURRENT_TIMESTAMP");
    doNothing().when(publisher).publishTaskCreated(any());
    relay().publishPending();
    verify(publisher, times(2)).publishTaskCreated(any());
    assertThat(count("outbox_events")).isZero();
  }

  private TaskOutboxRelay relay() {
    return new TaskOutboxRelay(jdbc, publisher, manager, registry, 20, 30);
  }

  @Test
  void gaugesReflectBacklogAndReturnToZeroAfterDelivery() {
    var pending = registry.get("task.outbox.pending").gauge();
    var age = registry.get("task.outbox.oldest.age").gauge();
    assertThat(pending.value()).isZero();
    assertThat(age.value()).isZero();
    service.create("owner", "catalog", "title", null, 1);
    jdbc.update("UPDATE outbox_events SET created_at = CURRENT_TIMESTAMP - INTERVAL '10 minutes'");
    assertThat(pending.value()).isEqualTo(1);
    assertThat(age.value()).isBetween(600.0, 610.0);
    relay().publishPending();
    assertThat(pending.value()).isZero();
    assertThat(age.value()).isZero();
  }

  @Test
  void anotherReplicaSkipsLockedEvent() {
    service.create("owner", "catalog", "title", null, 1);
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              jdbc.queryForList("SELECT task_id FROM outbox_events FOR UPDATE");
              try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
                try {
                  executor
                      .submit(() -> relay().publishPending())
                      .get(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception ex) {
                  throw new AssertionError("Relay should skip rows locked by another replica", ex);
                }
              }
              verifyNoInteractions(publisher);
            });
    relay().publishPending();
    verify(publisher).publishTaskCreated(any());
    assertThat(count("outbox_events")).isZero();
  }

  private int count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
  }
}
