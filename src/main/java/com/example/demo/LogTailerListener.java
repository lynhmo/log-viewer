package com.example.demo;

import org.apache.commons.io.input.TailerListenerAdapter;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class LogTailerListener extends TailerListenerAdapter {
    private final SimpMessagingTemplate messagingTemplate;
    private final String topicName;

    public LogTailerListener(SimpMessagingTemplate messagingTemplate, String logIdentifier) {
        this.messagingTemplate = messagingTemplate;
        this.topicName = "/topic/logs/" + logIdentifier;
    }

    @Override
    public void handle(String line) {
        // Gửi dòng log mới đến topic tương ứng trên Frontend
        messagingTemplate.convertAndSend(topicName, line);
    }

    @Override
    public void handle(Exception ex) {
        messagingTemplate.convertAndSend(topicName, "LỖI HỆ THỐNG: " + ex.getMessage());
    }
}