package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class LogseeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogseeApplication.class, args);
        log.info("LogSee Application started");
    }
}
