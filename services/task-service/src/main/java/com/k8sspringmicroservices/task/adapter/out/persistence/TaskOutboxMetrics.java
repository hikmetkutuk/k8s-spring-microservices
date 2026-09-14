package com.k8sspringmicroservices.task.adapter.out.persistence;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TaskOutboxMetrics implements MeterBinder {
  private final JdbcTemplate jdbc;

  public TaskOutboxMetrics(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    // Every replica reads the same database: aggregate these gauges with max, not sum.
    Gauge.builder(
            "task.outbox.pending",
            jdbc,
            db -> db.queryForObject("SELECT count(*) FROM outbox_events", Long.class))
        .description("Events waiting for Kafka acknowledgement in the shared outbox")
        .register(registry);
    Gauge.builder(
            "task.outbox.oldest.age",
            jdbc,
            db ->
                db.queryForObject(
                    """
            SELECT COALESCE(GREATEST(EXTRACT(EPOCH FROM
              (CURRENT_TIMESTAMP - MIN(created_at))), 0), 0) FROM outbox_events
            """,
                    Double.class))
        .baseUnit("seconds")
        .description("Age of the oldest pending event; zero when the outbox is empty")
        .register(registry);
  }
}
