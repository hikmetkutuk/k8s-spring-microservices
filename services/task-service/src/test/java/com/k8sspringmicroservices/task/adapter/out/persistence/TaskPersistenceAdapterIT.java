package com.k8sspringmicroservices.task.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.k8sspringmicroservices.task.domain.Task;
import com.k8sspringmicroservices.task.domain.TaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TaskPersistenceAdapterIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void flywayProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired private TaskJpaRepository jpaRepository;

  private TaskPersistenceAdapter adapter;

  @Test
  void savesAndReloadsTask_roundTripsAllFields() {
    adapter = new TaskPersistenceAdapter(jpaRepository, new TaskMapper());

    Task task =
        new Task(
            UUID.randomUUID().toString(),
            "owner-uuid-1",
            "catalog-item-uuid-1",
            "Buy Gadgets",
            "Need to purchase gadgets for the project",
            5,
            TaskStatus.PENDING,
            Instant.now(),
            Instant.now());

    Task saved = adapter.save(task);

    assertThat(saved).isNotNull();
    assertThat(saved.id()).isEqualTo(task.id());
    assertThat(saved.ownerId()).isEqualTo("owner-uuid-1");
    assertThat(saved.catalogItemId()).isEqualTo("catalog-item-uuid-1");
    assertThat(saved.title()).isEqualTo("Buy Gadgets");
    assertThat(saved.description()).isEqualTo("Need to purchase gadgets for the project");
    assertThat(saved.quantity()).isEqualTo(5);
    assertThat(saved.status()).isEqualTo(TaskStatus.PENDING);

    Optional<Task> reloaded = adapter.findById(task.id());
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().id()).isEqualTo(task.id());
    assertThat(reloaded.get().ownerId()).isEqualTo(task.ownerId());
    assertThat(reloaded.get().status()).isEqualTo(task.status());

    assertThat(adapter.existsById(task.id())).isTrue();
  }

  @Test
  void findAllByOwnerId_returnsOnlyTasksForThatOwner() {
    adapter = new TaskPersistenceAdapter(jpaRepository, new TaskMapper());

    Task taskA1 =
        new Task(
            UUID.randomUUID().toString(),
            "owner-a",
            "catalog-1",
            "Task A1",
            null,
            1,
            TaskStatus.PENDING,
            Instant.now(),
            Instant.now());
    Task taskA2 =
        new Task(
            UUID.randomUUID().toString(),
            "owner-a",
            "catalog-2",
            "Task A2",
            null,
            2,
            TaskStatus.COMPLETED,
            Instant.now(),
            Instant.now());
    Task taskB1 =
        new Task(
            UUID.randomUUID().toString(),
            "owner-b",
            "catalog-3",
            "Task B1",
            null,
            3,
            TaskStatus.PENDING,
            Instant.now(),
            Instant.now());

    adapter.save(taskA1);
    adapter.save(taskA2);
    adapter.save(taskB1);

    List<Task> ownerATasks = adapter.findAllByOwnerId("owner-a");
    assertThat(ownerATasks).hasSize(2);
    assertThat(ownerATasks)
        .extracting(Task::id)
        .containsExactlyInAnyOrder(taskA1.id(), taskA2.id());
    assertThat(ownerATasks).allMatch(t -> t.ownerId().equals("owner-a"));
  }

  @Test
  void findAllByOwnerId_returnsEmptyList_whenNoTasksForOwner() {
    adapter = new TaskPersistenceAdapter(jpaRepository, new TaskMapper());

    List<Task> result = adapter.findAllByOwnerId("nonexistent-owner");

    assertThat(result).isEmpty();
  }

  @Test
  void findById_returnsEmpty_whenTaskDoesNotExist() {
    adapter = new TaskPersistenceAdapter(jpaRepository, new TaskMapper());

    assertThat(adapter.findById(UUID.randomUUID().toString())).isEmpty();
  }

  @Test
  void existsById_returnsFalse_whenTaskDoesNotExist() {
    adapter = new TaskPersistenceAdapter(jpaRepository, new TaskMapper());

    assertThat(adapter.existsById(UUID.randomUUID().toString())).isFalse();
  }

  @Test
  void deleteById_removesTask() {
    adapter = new TaskPersistenceAdapter(jpaRepository, new TaskMapper());

    Task task =
        new Task(
            UUID.randomUUID().toString(),
            "owner-x",
            "catalog-x",
            "To Delete",
            null,
            1,
            TaskStatus.PENDING,
            Instant.now(),
            Instant.now());

    adapter.save(task);
    assertThat(adapter.existsById(task.id())).isTrue();

    adapter.deleteById(task.id());

    assertThat(adapter.existsById(task.id())).isFalse();
    assertThat(adapter.findById(task.id())).isEmpty();
  }

  @Test
  void save_preservesTaskStatusEnum_forBothStatuses() {
    adapter = new TaskPersistenceAdapter(jpaRepository, new TaskMapper());

    Task pending =
        new Task(
            UUID.randomUUID().toString(),
            "owner-1",
            "catalog-1",
            "Pending Task",
            null,
            1,
            TaskStatus.PENDING,
            Instant.now(),
            Instant.now());

    Task completed =
        new Task(
            UUID.randomUUID().toString(),
            "owner-1",
            "catalog-2",
            "Completed Task",
            null,
            1,
            TaskStatus.COMPLETED,
            Instant.now(),
            Instant.now());

    adapter.save(pending);
    adapter.save(completed);

    assertThat(adapter.findById(pending.id()))
        .get()
        .extracting(Task::status)
        .isEqualTo(TaskStatus.PENDING);
    assertThat(adapter.findById(completed.id()))
        .get()
        .extracting(Task::status)
        .isEqualTo(TaskStatus.COMPLETED);
  }
}
