package com.example.demo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

//@Component // Uncomment để tự động generate sample logs khi start app
public class SampleLogGenerator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleLogGenerator.class);

    @Value("${app.log.base-path:./logs}")
    private String logBasePath;

    private static final DateTimeFormatter LOG_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final String[] LOG_LEVELS = {"INFO", "WARN", "ERROR", "DEBUG"};
    private static final String[] LOGGERS = {
        "com.example.service.OrderService",
        "com.example.controller.OrderController",
        "com.example.repository.OrderRepository",
        "com.example.service.PaymentService"
    };
    private static final String[] MESSAGES = {
        "Processing order request",
        "Order validation completed",
        "Database query executed successfully",
        "Payment transaction initiated",
        "Order status updated",
        "User authentication successful",
        "Cache hit for order data",
        "REST API call completed",
        "Transaction committed successfully",
        "Error processing payment - insufficient funds",
        "Warning: High response time detected",
        "Retrying failed operation"
    };

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 Starting Sample Log Generator...");

        // Generate logs for last 7 days
        for (int daysAgo = 6; daysAgo >= 0; daysAgo--) {
            LocalDate date = LocalDate.now().minusDays(daysAgo);
            generateLogsForDate(date, "order-service", 100);
            generateLogsForDate(date, "checkout-service", 80);
        }

        log.info("✅ Sample logs generated successfully!");
    }

    private void generateLogsForDate(LocalDate date, String serviceName, int logCount) {
        try {
            // Create archive directory if not exists
            Path archiveDir = Paths.get(logBasePath, serviceName, "archive");
            Files.createDirectories(archiveDir);

            // Create log file: app-yyyy-MM-dd.1.log
            String fileName = String.format("app-%s.1.log", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            File logFile = archiveDir.resolve(fileName).toFile();

            Random random = new Random();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
                for (int i = 0; i < logCount; i++) {
                    // Random time within the day
                    int hour = random.nextInt(24);
                    int minute = random.nextInt(60);
                    int second = random.nextInt(60);
                    int millis = random.nextInt(1000);

                    LocalDateTime timestamp = date.atTime(hour, minute, second, millis * 1_000_000);

                    String level = LOG_LEVELS[random.nextInt(LOG_LEVELS.length)];
                    String logger = LOGGERS[random.nextInt(LOGGERS.length)];
                    String message = MESSAGES[random.nextInt(MESSAGES.length)];
                    String traceId = generateTraceId();

                    // Format: yyyy-MM-dd HH:mm:ss.SSS  LEVEL [SERVICE] [traceId=XXX] logger : message
                    String logLine = String.format("%s  %-5s [%s] [traceId=%s] %s : %s",
                        timestamp.format(LOG_FORMATTER),
                        level,
                        serviceName,
                        traceId,
                        logger,
                        message
                    );

                    writer.write(logLine);
                    writer.newLine();
                }
            }

            log.info("✓ Generated {} logs for {} on {}", logCount, serviceName, date);

        } catch (IOException e) {
            log.error("Error generating sample logs", e);
        }
    }

    private String generateTraceId() {
        Random random = new Random();
        return String.format("%08x-%04x-%04x",
            random.nextInt(),
            random.nextInt(65536),
            random.nextInt(65536));
    }
}
