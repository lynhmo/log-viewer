package com.example.demo;

public class RefreshRateRequest {
    private long rate;

    public RefreshRateRequest() {
    }

    public RefreshRateRequest(long rate) {
        this.rate = rate;
    }

    public long getRate() {
        return rate;
    }

    public void setRate(long rate) {
        this.rate = rate;
    }
}
