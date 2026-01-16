package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Random;
import java.util.UUID;

@SpringBootApplication
public class Demo1Application {

    private static final Logger log = LoggerFactory.getLogger(Demo1Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Demo1Application.class, args);

        generateLogsContinuously();
    }

    private static void generateLogsContinuously() {
        Logger log = LoggerFactory.getLogger(Demo1Application.class);
        Random random = new Random();
        int counter = 0;

        while (true) {
            try {
                counter++;

                // traceId ngẫu nhiên cho mỗi vòng
                String traceId = UUID.randomUUID().toString().substring(0, 8);
                MDC.put("traceId", traceId);

                log.info("Create order request");
                Thread.sleep(100);

                log.info("Validate order");
                Thread.sleep(100);

                // Sinh WARN / ERROR có kiểm soát
                if (counter % 5 == 0) {
                    log.error("Out of stock exception");
                } else if (counter % 3 == 0) {
                    log.warn("Slow response detected");
                } else {
                    log.info("Order created successfully");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Log generator interrupted", e);
                break;
            } finally {
                MDC.clear();
            }

            // Nghỉ 1 giây trước vòng tiếp theo
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }


}
