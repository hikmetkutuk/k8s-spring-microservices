# Service and outbox alerts

The observability chart loads seven rules through `prometheus.serverFiles.alerts`.
This installation uses standalone Prometheus, so no Prometheus Operator or
`PrometheusRule` CRD is required. Rules become active after deploying the updated
chart; new application metrics require deploying the updated service images.

| Alert | Condition | Hold time |
| --- | --- | --- |
| TaskOutboxBacklog | More than 100 pending events | 5 minutes |
| TaskOutboxOldestEvent | Oldest pending event exceeds 300 seconds | 2 minutes |
| TaskOutboxPublishFailures | Failed publication attempts in the last 5 minutes | 2 minutes |
| MicroserviceUnavailable | Deployment has zero available replicas | 2 minutes |
| MicroserviceScrapeFailed | All discovered replicas fail scraping | 2 minutes |
| MicroserviceHighErrorRate | HTTP 5xx ratio exceeds 5% | 5 minutes |
| MicroserviceHighLatency | HTTP p99 exceeds 2 seconds | 5 minutes |

HTTP alerts exclude actuator requests and require at least 20 requests in the
last five minutes. Thresholds are initial local-development defaults in
`helm/observability/values.yaml`; tune them against measured traffic before using
them as production objectives. All six services publish HTTP histogram buckets
and the `application` label. Kubernetes discovery supplies `namespace`.

## Outbox metrics

- `task_outbox_pending`: database-wide pending event count.
- `task_outbox_oldest_age_seconds`: age of the oldest pending event, zero when empty.
- `task_outbox_publish_failures_total`: failed Kafka send attempts per process,
  including retries and ambiguous timeouts; resets on process restart.

Each task-service replica reads the same database when its gauges are scraped.
Rules use `max` across replicas for these gauges to avoid double counting, and
sum counter increases across replicas for failures. Gauge reads run SQL aggregate
queries during scraping; a large backlog increases database work. Database query
failure is not reported as a healthy zero (Micrometer reports an unavailable gauge).

## Investigation

For outbox alarms, inspect task-service logs, Kafka availability, and PostgreSQL
`outbox_events.attempts` / `next_attempt_at`. Keep pending rows for retry. After
recovery, pending count and oldest age should fall to zero. The failure alarm can
remain active until failed attempts leave its five-minute window.

For availability alarms, inspect Deployment status, pod events, readiness probes,
and scrape targets. The deployment rule covers the zero-pod case that an `up == 0`
rule alone misses. Deleting a Deployment or losing kube-state-metrics entirely is
not covered by this rule. HTTP alarms should be investigated using logs and traces.

Alerts are visible in Prometheus at `/alerts`. Alertmanager remains disabled in
this local stack: no email, Slack, or other external notifications are sent.

## Verification

Requires Helm, Docker, Python 3 and PyYAML:

```sh
python3 helm/observability/tests/test_alerts.py
./gradlew :services:task-service:test :services:task-service:spotlessCheck
```

The script renders the real chart ConfigMap, verifies the rule file is loaded,
and runs `promtool check rules` and `promtool test rules` using the rendered
Prometheus image version. Controlled time series cover firing, hold times,
recovery, low traffic, and duplicate replica gauges. These tests do not interrupt
a running Kafka broker or deploy changes to a cluster.

References: [Prometheus alerting rules](https://prometheus.io/docs/prometheus/latest/configuration/alerting_rules/),
[promtool rule tests](https://prometheus.io/docs/prometheus/latest/configuration/unit_testing_rules/),
[Micrometer histograms](https://docs.micrometer.io/micrometer/reference/concepts/histogram-quantiles.html).
