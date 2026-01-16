# Monitoring Plan (OLA 5)

## Goal
The goal of this monitoring setup is to continuously measure whether the MTOGO API is meeting its reliability targets using a small set of high-signal metrics. The dashboard focuses on:
- Availability (uptime proxy)
- Request traffic (rate)
- Failure rate (errors)
- Request latency distribution (duration)

This follows two widely used monitoring perspectives:
- **RED** for request-driven services: Rate, Errors, Duration. :contentReference[oaicite:0]{index=0}
- **USE** for infrastructure/resources: Utilization, Saturation, Errors. :contentReference[oaicite:1]{index=1}


## System scope
This OLA 5 setup monitors:
- Spring Boot service (mtogo-api) exposing `/orders` and metrics via `/actuator/prometheus`
- Prometheus scraping the metrics endpoint
- Grafana dashboard querying Prometheus
- cAdvisor exporting container resource metrics (CPU/memory)

Prometheus is responsible for collecting time-series, and Grafana is used for visualization.


## RED metrics (service-level / user-facing signals)
RED is used per endpoint (primarily `/orders`) because it directly reflects user experience and service health. :contentReference[oaicite:2]{index=2}

### Rate (traffic)
**What:** requests per second (RPS) for the service.  
**Why:** proves the service is receiving traffic; helps correlate load spikes with latency/errors.

**Metric/query used:**
- `sum(rate(mtogo_orders_requests_total[5m]))`

### Errors (failures)
**What:** percentage of requests returning 5xx (simulated failures in this project).  
**Why:** directly supports availability/error-rate SLOs.

**Metric/query used:**
- `(
  sum(rate(mtogo_orders_requests_total{status="500"}[5m]))
  /
  sum(rate(mtogo_orders_requests_total[5m]))
) * 100`

### Duration (latency)
**What:** latency distribution, tracked via histogram quantiles (p95).  
**Why:** an average hides tail latency; percentiles provide an SLO-friendly view.

**Metric/query used (p95):**
- `histogram_quantile(
  0.95,
  sum(rate(mtogo_orders_request_duration_seconds_bucket[5m])) by (le)
)`


## USE metrics (resource-level signals)
USE is used for system resources to identify bottlenecks (CPU/memory). :contentReference[oaicite:3]{index=3}

### Utilization
**What:** CPU usage and memory usage per container.  
**Why:** helps identify whether performance problems are driven by resource constraints.

**Metrics/queries (cAdvisor):**
- CPU (per container):  
  `sum(rate(container_cpu_usage_seconds_total{container!="",container!="POD"}[1m])) by (container)`
- Memory (per container):  
  `sum(container_memory_working_set_bytes{container!="",container!="POD"}) by (container)`

### Saturation
**What:** saturation indicates queued work and bottlenecks (e.g., CPU throttling, queue depth, thread pool saturation, DB connection pool usage).  
**Why:** high utilization alone may not indicate an issue; saturation is stronger evidence of a bottleneck.

**Note for this iteration:** The project does not include DB/queue/thread pool saturation metrics yet. In a future version, I would add:
- server thread pool metrics
- DB connection pool utilization (if a database is introduced)

### Errors
**What:** container restarts/OOM events and scrape target down events.  
**Why:** indicates instability/failure at the infrastructure or service layer.

**Signals:**
- `up{job="mtogo-api"}` (service scrape availability)


## Alerting approach
Prometheus alert rules are defined in `prometheus/prometheus.rules.yml` using `expr`, `for`, `labels`, and `annotations`. :contentReference[oaicite:4]{index=4}

Alerts configured:
- **AppDown**: `up{job="mtogo-api"} == 0`
- **HighErrorRate**: error ratio > 5% for 1 minute
- **SlowRequestsP95**: p95 latency > 0.8s for 1 minute

The alerts are intentionally simple and aligned with RED metrics so they can be demonstrated by stopping the app container/process, simulating 5xx responses, or simulating slow requests.
