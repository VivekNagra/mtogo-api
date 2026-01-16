## OLA 5: Monitoring + Alerts (Prometheus + Grafana)

### Run the application
Start the Spring Boot app in IntelliJ (or run from terminal):

```powershell
.\mvnw spring-boot:run
```
Verify endpoints:

- http://localhost:8080/orders
- http://localhost:8080/actuator/health
- http://localhost:8080/actuator/prometheus

### Run the monitoring stack
From the project root:
```powershell 
docker compose up -d
```

### Open:
- Prometheus: http://localhost:9090
    - (Status → Targets should show mtogo-api = UP)
- Grafana: http://localhost:3000
- kode og login: admin / admin)

### Generate traffic (so metrics appear)
Normal traffic:
```Powershell
1..200 | ForEach-Object {
Invoke-WebRequest "http://localhost:8080/orders" -UseBasicParsing | Out-Null
Start-Sleep -Milliseconds 100 }
```
Simulate errors (for error-rate panel and HighErrorRate alert):
```powershell
1..120 | ForEach-Object {
  try { Invoke-WebRequest "http://localhost:8080/orders?fail=true" -UseBasicParsing | Out-Null } catch {}
  Start-Sleep -Milliseconds 100}
```
Simulate slowness (for latency panel and SlowRequestsP95 alert):
```powershell
1..60 | ForEach-Object {
  Invoke-WebRequest "http://localhost:8080/orders?delayMs=1200" -UseBasicParsing | Out-Null
  Start-Sleep -Milliseconds 150}
```
# Demo Checklist: Monitoring & Observability

### Prometheus Targets
* [ ] Verify mtogo-api status is UP in the targets list.

### Grafana Dashboard
Check for the presence and real-time data of the following panels:
* [ ] Service Status: (Up/Down)
* [ ] Throughput: Requests Per Second (RPS)
* [ ] Error Rate: (%)
* [ ] Latency: p95 panels

---

### Trigger Controlled Failures
Follow these steps to validate alerting logic:

1. Stop the application
  * Expected Outcome: AppDown alert triggers.
2. Run fail=true traffic
  * Expected Outcome: HighErrorRate alert triggers.
3. Run delayMs traffic
  * Expected Outcome: SlowRequestsP95 alert triggers.

---

### Alert Verification
* Prometheus Alerts: http://localhost:9090/alerts
  * Ensure alerts move from "Pending" to "Firing".

---

### Supporting Documentation
Detailed technical specifications can be found in:
* docs/monitoring-plan.md
* docs/nfrs-kpis-sla.md

--- 

## 5) Screenshot checklist

Save your evidence screenshots in: `docs/screenshots/`

Recommended screenshots (high value for grading):
1. **Prometheus → Status → Targets** showing `mtogo-api` = **UP**
2. **Prometheus → Alerts** showing at least one alert **Pending** or **Firing** (e.g., `HighErrorRate`)
3. **Grafana dashboard** showing the four panels with values:
  - `up{job="mtogo-api"}`
  - Orders RPS
  - Orders error rate %
  - Orders p95 latency
4. Browser view of `http://localhost:8080/actuator/prometheus` to show the metrics endpoint is exposed



