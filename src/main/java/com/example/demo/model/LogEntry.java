package com.example.demo.model;

import java.time.LocalDateTime;

public class LogEntry {
    private LocalDateTime timestamp;
    private String level;
    private String serviceName;
    private String traceId;
    private String logger;
    private String message;
    private String rawLine;

    public LogEntry() {
    }

    public LogEntry(LocalDateTime timestamp, String level, String serviceName, String traceId,
                    String logger, String message, String rawLine) {
        this.timestamp = timestamp;
        this.level = level;
        this.serviceName = serviceName;
        this.traceId = traceId;
        this.logger = logger;
        this.message = message;
        this.rawLine = rawLine;
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRawLine() {
        return rawLine;
    }

    public void setRawLine(String rawLine) {
        this.rawLine = rawLine;
    }
}
