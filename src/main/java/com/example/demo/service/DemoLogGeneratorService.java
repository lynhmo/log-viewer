package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.UUID;

/**
 * Generates sample log data for demo purposes.
 * Runs in a background thread so it doesn't block the main application.
 * Disabled by default; enable by setting {@code app.demo.log-generator.enabled=true}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.demo.log-generator.enabled", havingValue = "true", matchIfMissing = false)
public class DemoLogGeneratorService {

    private volatile boolean running = true;

    @PostConstruct
    public void start() {
        Thread generatorThread = new Thread(this::generateLogsContinuously);
        generatorThread.setDaemon(true);
        generatorThread.setName("DemoLogGenerator");
        generatorThread.setUncaughtExceptionHandler((t, e) ->
            log.error("Uncaught exception in demo log generator thread: {}", t.getName(), e));
        generatorThread.start();
        log.info("Demo log generator started in background thread");
    }

    private void generateLogsContinuously() {
        int counter = 0;

        while (running) {
            try {
                counter++;

                // Random traceId for each cycle
                String traceId = UUID.randomUUID().toString().substring(0, 8);
                MDC.put("traceId", traceId);

                log.info("Create order request");
                Thread.sleep(100);

                log.info("Validate order");
                Thread.sleep(100);

                // Generate WARN / ERROR with controlled frequency
                if (counter % 5 == 0) {
                    log.error("Out of stock exception");
                } else if (counter % 3 == 0) {
                    log.warn("Slow response detected");
                } else {
                    log.info("Order created successfully");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Demo log generator interrupted");
                break;
            } finally {
                MDC.clear();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Gracefully stop the log generator. Called during application shutdown.
     */
    public void stop() {
        running = false;
        log.info("Demo log generator stopping");
    }
}
