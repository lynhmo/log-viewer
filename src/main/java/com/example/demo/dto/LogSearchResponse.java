package com.example.demo.dto;

import com.example.demo.model.LogEntry;

import java.util.List;

public class LogSearchResponse {
    private List<LogEntry> logs;
    private int totalCount;
    private String searchDate;

    public LogSearchResponse() {
    }

    public LogSearchResponse(List<LogEntry> logs, int totalCount, String searchDate) {
        this.logs = logs;
        this.totalCount = totalCount;
        this.searchDate = searchDate;
    }

    public List<LogEntry> getLogs() {
        return logs;
    }

    public void setLogs(List<LogEntry> logs) {
        this.logs = logs;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public String getSearchDate() {
        return searchDate;
    }

    public void setSearchDate(String searchDate) {
        this.searchDate = searchDate;
    }
}
