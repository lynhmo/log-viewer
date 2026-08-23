package com.example.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LogWebSocketController {

    private final DynamicLogManagerService logManagerService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Xử lý yêu cầu cập nhật refresh rate từ client
     */
    @MessageMapping("/update-refresh-rate")
    public void updateRefreshRate(RefreshRateRequest request) {
        try {
            long newRate = request.getRate();
            log.info("Received refresh rate update request: {}ms", newRate);

            logManagerService.updateRefreshRate(newRate);

            // Thông báo cho tất cả client biết refresh rate đã thay đổi
            messagingTemplate.convertAndSend("/topic/refresh-rate-updated",
                "Refresh rate updated: " + newRate + "ms");

        } catch (Exception e) {
            log.error("Error updating refresh rate: {}", e.getMessage(), e);

            // Gửi thông báo lỗi
            messagingTemplate.convertAndSend("/topic/refresh-rate-error",
                "Error updating refresh rate: " + e.getMessage());
        }
    }
}
