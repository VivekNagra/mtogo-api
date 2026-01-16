package dk.cba.mtogo.mtogoapi.api;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final MeterRegistry registry;

    public OrderController(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Simulates an order endpoint you can use to demonstrate:
     * - Rate (traffic): request count
     * - Errors: 5xx responses
     * - Duration: latency distribution
     *
     * Try:
     *  /orders?fail=true
     *  /orders?delayMs=1200
     */
    @GetMapping
    public ResponseEntity<String> getOrders(
            @RequestParam(defaultValue = "false") boolean fail,
            @RequestParam(defaultValue = "0") long delayMs
    ) {

        Timer.Sample sample = Timer.start(registry);

        try {
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            } else {
                // Small random delay so graphs are not flat
                Thread.sleep(ThreadLocalRandom.current().nextInt(10, 80));
            }

            if (fail) {
                registry.counter("mtogo_orders_requests_total", "status", "500").increment();
                return ResponseEntity.status(500).body("Simulated server error");
            }

            registry.counter("mtogo_orders_requests_total", "status", "200").increment();
            return ResponseEntity.ok("Orders OK");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            registry.counter("mtogo_orders_requests_total", "status", "500").increment();
            return ResponseEntity.status(500).body("Interrupted");

        } finally {
            sample.stop(Timer.builder("mtogo_orders_request_duration_seconds")
                    .description("Order endpoint request duration")
                    .publishPercentileHistogram(true)
                    .serviceLevelObjectives(
                            Duration.ofMillis(100),
                            Duration.ofMillis(300),
                            Duration.ofMillis(800),
                            Duration.ofSeconds(2)
                    )
                    .register(registry));
        }
    }
}
