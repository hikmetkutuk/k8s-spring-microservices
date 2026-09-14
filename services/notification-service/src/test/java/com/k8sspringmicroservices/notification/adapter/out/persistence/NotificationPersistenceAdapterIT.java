package com.k8sspringmicroservices.notification.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.k8sspringmicroservices.notification.domain.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class NotificationPersistenceAdapterIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void flywayProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired private NotificationJpaRepository jpaRepository;

  private NotificationPersistenceAdapter adapter;

  @Test
  void redeliveredTaskEventCreatesOnlyOneNotification() {
    adapter = new NotificationPersistenceAdapter(jpaRepository, new NotificationMapper());
    var service =
        new com.k8sspringmicroservices.notification.application.service.NotificationService(
            adapter);
    var event =
        new com.k8sspringmicroservices.common.event.TaskCreatedEvent(
            "redelivered-task", "owner", "catalog", "title", 1, Instant.now());
    service.handleTaskCreated(event);
    jpaRepository.flush();
    service.handleTaskCreated(event);
    jpaRepository.flush();
    assertThat(adapter.findAllByOwnerId("owner")).hasSize(1);
  }

  @Test
  void savesAndReloadsNotification_roundTripsAllFields() {
    adapter = new NotificationPersistenceAdapter(jpaRepository, new NotificationMapper());

    Notification notification =
        new Notification(
            UUID.randomUUID().toString(),
            "task-uuid-abc",
            "owner-uuid-xyz",
            "Task created: Buy Gadgets",
            Instant.now());

    Notification saved = adapter.save(notification);

    assertThat(saved).isNotNull();
    assertThat(saved.id()).isEqualTo(notification.id());
    assertThat(saved.taskId()).isEqualTo("task-uuid-abc");
    assertThat(saved.ownerId()).isEqualTo("owner-uuid-xyz");
    assertThat(saved.message()).isEqualTo("Task created: Buy Gadgets");

    assertThat(adapter.existsByTaskId("task-uuid-abc")).isTrue();

    List<Notification> ownerNotifications = adapter.findAllByOwnerId("owner-uuid-xyz");
    assertThat(ownerNotifications).hasSize(1);
    assertThat(ownerNotifications.get(0).id()).isEqualTo(notification.id());
  }

  @Test
  void existsByTaskId_returnsFalse_whenNotificationDoesNotExist() {
    adapter = new NotificationPersistenceAdapter(jpaRepository, new NotificationMapper());

    assertThat(adapter.existsByTaskId("nonexistent-task-id")).isFalse();
  }

  @Test
  void findAllByOwnerId_returnsOnlyNotificationsForThatOwner() {
    adapter = new NotificationPersistenceAdapter(jpaRepository, new NotificationMapper());

    Notification n1 =
        new Notification(UUID.randomUUID().toString(), "task-1", "owner-1", "msg 1", Instant.now());
    Notification n2 =
        new Notification(UUID.randomUUID().toString(), "task-2", "owner-1", "msg 2", Instant.now());
    Notification n3 =
        new Notification(UUID.randomUUID().toString(), "task-3", "owner-2", "msg 3", Instant.now());

    adapter.save(n1);
    adapter.save(n2);
    adapter.save(n3);

    List<Notification> owner1Notifications = adapter.findAllByOwnerId("owner-1");
    assertThat(owner1Notifications).hasSize(2);
    assertThat(owner1Notifications)
        .extracting(Notification::id)
        .containsExactlyInAnyOrder(n1.id(), n2.id());

    List<Notification> owner2Notifications = adapter.findAllByOwnerId("owner-2");
    assertThat(owner2Notifications).hasSize(1);
    assertThat(owner2Notifications.get(0).id()).isEqualTo(n3.id());
  }

  @Test
  void findAllByOwnerId_returnsEmptyList_whenNoNotificationsForOwner() {
    adapter = new NotificationPersistenceAdapter(jpaRepository, new NotificationMapper());

    assertThat(adapter.findAllByOwnerId("nonexistent")).isEmpty();
  }

  @Test
  void save_throwsDataIntegrityViolation_whenDuplicateTaskId() {
    adapter = new NotificationPersistenceAdapter(jpaRepository, new NotificationMapper());

    Notification first =
        new Notification(
            UUID.randomUUID().toString(), "same-task-id", "owner-1", "first", Instant.now());
    Notification second =
        new Notification(
            UUID.randomUUID().toString(), "same-task-id", "owner-2", "second", Instant.now());

    adapter.save(first);
    // @DataJpaTest transactional olduğu için save() hemen flush etmez —
    // INSERT'ler transaction commit'te veya manuel flush'ta çalışır.
    jpaRepository.flush();

    assertThatThrownBy(
            () -> {
              adapter.save(second);
              jpaRepository.flush();
            })
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
