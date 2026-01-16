package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class LogWebSocketController {

    @Autowired
    private DynamicLogManagerService logManagerService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Xử lý yêu cầu cập nhật refresh rate từ client
     */
    @MessageMapping("/update-refresh-rate")
    public void updateRefreshRate(RefreshRateRequest request) {
        try {
            long newRate = request.getRate();
            System.out.println("Nhận yêu cầu cập nhật refresh rate: " + newRate + "ms");

            logManagerService.updateRefreshRate(newRate);

            // Thông báo cho tất cả client biết refresh rate đã thay đổi
            messagingTemplate.convertAndSend("/topic/refresh-rate-updated",
                "Refresh rate đã được cập nhật: " + newRate + "ms");

        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật refresh rate: " + e.getMessage());
            e.printStackTrace();

            // Gửi thông báo lỗi
            messagingTemplate.convertAndSend("/topic/refresh-rate-error",
                "Lỗi khi cập nhật refresh rate: " + e.getMessage());
        }
    }
}
