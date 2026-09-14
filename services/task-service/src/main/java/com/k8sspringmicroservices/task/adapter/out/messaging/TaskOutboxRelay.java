package com.k8sspringmicroservices.task.adapter.out.messaging;

import com.k8sspringmicroservices.common.event.TaskCreatedEvent;
import com.k8sspringmicroservices.task.application.port.out.TaskEventPublisherPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TaskOutboxRelay {
  private static final Logger log = LoggerFactory.getLogger(TaskOutboxRelay.class);
  private final JdbcTemplate jdbc;
  private final TaskEventPublisherPort publisher;
  private final TransactionTemplate transaction;
  private final int batchSize;
  private final int retryDelaySeconds;
  private final Counter publishFailures;

  public TaskOutboxRelay(
      JdbcTemplate jdbc,
      TaskEventPublisherPort publisher,
      PlatformTransactionManager transactionManager,
      MeterRegistry registry,
      @Value("${outbox.batch-size:20}") int batchSize,
      @Value("${outbox.retry-delay-seconds:30}") int retryDelaySeconds) {
    if (batchSize < 1 || retryDelaySeconds < 1) {
      throw new IllegalArgumentException("Outbox batch size and retry delay must be positive");
    }
    this.jdbc = jdbc;
    this.publisher = publisher;
    this.transaction = new TransactionTemplate(transactionManager);
    this.batchSize = batchSize;
    this.retryDelaySeconds = retryDelaySeconds;
    this.publishFailures =
        Counter.builder("task.outbox.publish.failures")
            .description("Failed Kafka publication attempts, including retries")
            .register(registry);
  }

  @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:1000}")
  public void publishPending() {
    for (int i = 0; i < batchSize; i++) {
      if (!Boolean.TRUE.equals(transaction.execute(status -> publishNext()))) {
        break;
      }
    }
  }

  private boolean publishNext() {
    // Hold one row lock through broker acknowledgement; other replicas skip this row.
    var events =
        jdbc.query(
            """
        SELECT * FROM outbox_events WHERE next_attempt_at <= CURRENT_TIMESTAMP
        ORDER BY next_attempt_at, created_at LIMIT 1 FOR UPDATE SKIP LOCKED
        """,
            (rs, row) ->
                new TaskCreatedEvent(
                    rs.getString("task_id"),
                    rs.getString("owner_id"),
                    rs.getString("catalog_item_id"),
                    rs.getString("title"),
                    rs.getInt("quantity"),
                    rs.getTimestamp("created_at").toInstant()));
    if (events.isEmpty()) {
      return false;
    }
    var event = events.getFirst();
    try {
      publisher.publishTaskCreated(event);
    } catch (Exception ex) {
      publishFailures.increment();
      jdbc.update(
          """
          UPDATE outbox_events SET attempts = attempts + 1,
          next_attempt_at = CURRENT_TIMESTAMP + (? * INTERVAL '1 second') WHERE task_id = ?
          """,
          retryDelaySeconds,
          event.taskId());
      log.warn("Outbox publish failed for taskId={}; will retry", event.taskId(), ex);
      return true;
    }
    // A crash between Kafka acknowledgement and commit can redeliver the event.
    jdbc.update("DELETE FROM outbox_events WHERE task_id = ?", event.taskId());
    return true;
  }
}
