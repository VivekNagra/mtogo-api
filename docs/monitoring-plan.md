# Monitoring Plan (OLA 5)

## Goal
The goal of this monitoring setup is to continuously measure whether the MTOGO API is meeting its reliability targets using a small set of high-signal metrics. The dashboard focuses on:
- Availability (uptime proxy)
- Request traffic (rate)
- Failure rate (errors)
- Request latency distribution (duration)

This monitoring setup uses two common perspectives:
- **RED** for request-driven services: Rate, Errors, Duration
- **USE** for infrastructure/resources: Utilization, Saturation, Errors

## System scope
This OLA 5 setup monitors:
- Spring Boot service (mtogo-api) exposing `/orders` and metrics via `/actuator/prometheus`
- Prometheus scraping the metrics endpoint
- Grafana dashboard querying Prometheus
- cAdvisor exporting container resource metrics (CPU/memory)

Prometheus collects time-series metrics. Grafana visualizes those metrics in a dashboard.

## RED metrics (service-level / user-facing signals)

### Rate (traffic)
**What:** requests per second (RPS).  
**Why:** confirms traffic and helps correlate load with latency/errors.

**Query used:**
- `sum(rate(mtogo_orders_requests_total[5m]))`

### Errors (failures)
**What:** error percentage (simulated 5xx in this project).  
**Why:** supports an error-rate SLO and highlights instability.

**Query used:**
- `(
  sum(rate(mtogo_orders_requests_total{status="500"}[5m]))
  /
  sum(rate(mtogo_orders_requests_total[5m]))
) * 100`

### Duration (latency)
**What:** tail latency via p95 from histogram buckets.  
**Why:** percentiles show user experience better than averages.

**Query used (p95):**
- `histogram_quantile(
  0.95,
  sum(rate(mtogo_orders_request_duration_seconds_bucket[5m])) by (le)
)`

## USE metrics (resource-level signals)

### Utilization
**What:** CPU and memory usage per container.  
**Why:** helps diagnose performance issues caused by resource limits.

**Queries (cAdvisor):**
- CPU:
  - `sum(rate(container_cpu_usage_seconds_total{container!="",container!="POD"}[1m])) by (container)`
- Memory:
  - `sum(container_memory_working_set_bytes{container!="",container!="POD"}) by (container)`

### Saturation
**What:** backlog/queueing indicators (thread pool saturation, DB pool usage, CPU throttling).  
**Why:** high utilization alone isn’t always a problem; saturation signals bottlenecks.

**Note for this iteration:** saturation metrics are not implemented yet because the service has no DB/queue. A realistic next step would be adding thread pool and connection pool metrics.

### Errors
**What:** service down/unreachable and container stability signals.  
**Why:** indicates failures at service or infrastructure level.

**Signal used:**
- `up{job="mtogo-api"}`

## Alerting approach
Prometheus alert rules are defined in `prometheus/prometheus.rules.yml` using `expr`, `for`, `labels`, and `annotations`.

Alerts configured:
- **AppDown**: `up{job="mtogo-api"} == 0`
- **HighErrorRate**: error ratio > 5% for 1 minute
- **SlowRequestsP95**: p95 latency > 0.8s for 1 minute

These alerts are aligned with the RED metrics and can be demonstrated by stopping the app, simulating 5xx responses, or simulating slow responses.
