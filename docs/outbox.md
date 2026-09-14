# Task event outbox

Task creation writes the task and its `TaskCreatedEvent` to PostgreSQL in the
same transaction. The `BEFORE_COMMIT` listener writes `outbox_events`; an insert
failure rolls back task creation. Kafka availability does not affect this commit.

The scheduled relay locks one due row using `FOR UPDATE SKIP LOCKED`, waits for
Kafka acknowledgement, then deletes that row. Each row has its own transaction.
Multiple task-service replicas can run relays concurrently. Failed sends increment
`attempts` and move `next_attempt_at` forward; retries have no maximum attempt limit.
Pending rows survive service restarts and are independent of task deletion.

Delivery is at least once: a crash after Kafka acknowledgement but before the
database commit, or an ambiguous send timeout, can deliver the same event again.
Notification-service deduplicates by task ID, backed by a unique database constraint.
There is no global delivery ordering guarantee.

| Environment variable | Default | Meaning |
| --- | --- | --- |
| `OUTBOX_POLL_INTERVAL_MS` | `1000` | Delay between completed polling batches |
| `OUTBOX_BATCH_SIZE` | `20` | Maximum rows attempted per poll |
| `OUTBOX_RETRY_DELAY_SECONDS` | `30` | Delay after failed publication |

Successful rows are deleted, so the table represents pending delivery only.
Monitor the pending count, oldest `created_at`, and `attempts` for backlog growth.
Kafka producer blocking is bounded to 5 seconds and acknowledgement waiting to
10 seconds; database row locks are held during publication.

Run task and notification tests (Docker required for Testcontainers):

```sh
./gradlew :services:task-service:test :services:notification-service:test
```

The outbox integration tests use PostgreSQL to check atomic commit/rollback,
outbox insert failure, persisted retry state across relay recreation, retry delay,
and concurrent replica locking. Broker failure is injected at the publisher port;
the Kafka publisher unit test checks asynchronous send failure propagation.
Notification persistence tests check duplicate delivery against PostgreSQL.
