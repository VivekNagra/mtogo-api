# NFRs, KPIs, SLIs/SLOs, and SLA (OLA 5)

## Non-Functional Requirements (NFRs)

| Category | Requirement | Why it matters | How it is measured (in this repo) |
|---|---|---|---|
| Availability | The API should be reachable and operational most of the time. | Users must be able to place orders. | `up{job="mtogo-api"}` in Prometheus/Grafana |
| Latency | Requests should complete quickly for a good UX. | Slow responses degrade user experience. | p95 via histogram quantile of `mtogo_orders_request_duration_seconds_bucket` |
| Error rate | Server-side failures should remain low. | Failed requests reduce trust and reliability. | Error % from `mtogo_orders_requests_total{status="500"}` / total |
| Observability | Metrics and dashboards must exist to verify behavior. | Enables monitoring and incident diagnosis. | `/actuator/prometheus`, Prometheus scraping, Grafana dashboard panels |
| Maintainability | Monitoring setup must be reproducible from repo. | Grader should be able to run it. | `docker compose up -d` and README steps |

---

## KPIs (Business-facing)

These are “business outcome” indicators (not all are implemented as metrics in this iteration):

- Orders per hour/day
- Conversion rate (sessions → orders)
- Average order value
- Refund/complaint rate

---

## SLIs (Service Level Indicators)

SLIs are the measurable signals we use to judge service quality:

1) **Availability SLI**
- Signal: `up{job="mtogo-api"}`
- Meaning: 1 = Prometheus can scrape the service; 0 = not reachable/down.

2) **Error-rate SLI**
- Signal: percentage of 5xx responses on `/orders`
- PromQL (same logic used in Grafana):
    - `(
      sum(rate(mtogo_orders_requests_total{status="500"}[5m]))
      /
      sum(rate(mtogo_orders_requests_total[5m]))
    ) * 100`

3) **Latency SLI**
- Signal: p95 request latency for `/orders`
- PromQL:
    - `histogram_quantile(
      0.95,
      sum(rate(mtogo_orders_request_duration_seconds_bucket[5m])) by (le)
    )`

---

## SLOs (Service Level Objectives)

The SLOs below are realistic targets for this small API iteration:

- **Availability SLO:** ≥ **99.5%** per month (proxy: `up{job="mtogo-api"} == 1`)
- **Error-rate SLO:** ≤ **0.5%** 5xx over rolling windows (Grafana error-rate panel)
- **Latency SLO:** p95 latency for `/orders` < **0.8s** (matches alert threshold)

---

## SLA (Service Level Agreement statement)

Project-version SLA statement:

> The MTOGO Ordering API is available 99.5% of the time per month, measured via continuous monitoring of service availability. The service aims to keep server-side failures below 0.5% and to keep the p95 latency for `/orders` below 0.8 seconds, as tracked in the Grafana dashboard backed by Prometheus metrics.
