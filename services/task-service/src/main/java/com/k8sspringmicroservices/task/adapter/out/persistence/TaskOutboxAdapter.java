package com.k8sspringmicroservices.task.adapter.out.persistence;

import com.k8sspringmicroservices.common.event.TaskCreatedEvent;
import com.k8sspringmicroservices.task.application.port.out.TaskOutboxPort;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TaskOutboxAdapter implements TaskOutboxPort {
  private final JdbcTemplate jdbc;

  public TaskOutboxAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void append(TaskCreatedEvent event) {
    jdbc.update(
        """
        INSERT INTO outbox_events (task_id, owner_id, catalog_item_id, title, quantity, created_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        event.taskId(),
        event.ownerId(),
        event.catalogItemId(),
        event.title(),
        event.quantity(),
        Timestamp.from(event.createdAt()));
  }
}
