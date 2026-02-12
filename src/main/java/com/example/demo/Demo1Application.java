package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
public class Demo1Application {

    public static void main(String[] args) {
        SpringApplication.run(Demo1Application.class, args);
        generateLogsContinuously();
    }

    private static void generateLogsContinuously() {
        Logger log = LoggerFactory.getLogger(Demo1Application.class);
        int counter = 0;

        while (true) {
            try {
                counter++;

                // traceId ngẫu nhiên cho mỗi vòng
                String traceId = UUID.randomUUID().toString().substring(0, 8);
                MDC.put("traceId", traceId);

                log.info("Create order request");
                log.info("Tiếng việt ở đây thì sao nhỉ?");
                Thread.sleep(100);

                log.info("Validate");
                Thread.sleep(100);

                // Sinh WARN / ERROR có kiểm soát
                if (counter % 5 == 0) {
                    log.error("Out of stock exception");
                } else if (counter % 3 == 0) {
                    log.debug("Payment processing");
                    // log.warn("Slow response detected");
                } else {
                    log.warn("Order created successfully");
                }

                if (counter % 5 == 0) {
                    throw new RuntimeException("Simulated exception for testing");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Log generator interrupted", e);
                break;
            } catch (RuntimeException e) {
                log.error("Simulated exception occurred", e);
                // e.printStackTrace();
            } finally {
                MDC.clear();
            }

            // Nghỉ 1 giây trước vòng tiếp theo
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }


}
