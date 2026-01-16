package com.example.demo.dto;

public class LogSearchRequest {
    private String date; // yyyy-MM-dd
    private String serviceName; // order-service hoặc checkout-service
    private Integer limit = 1000; // số lượng log entries mặc định

    // New optional time range (on the given date)
    // Accept formats: HH:mm or HH:mm:ss
    private String startTime;
    private String endTime;

    public LogSearchRequest() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
