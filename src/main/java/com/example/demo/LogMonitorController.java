package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LogMonitorController {

    @Autowired
    private DynamicLogManagerService logManagerService;

    /**
     * Endpoint để kiểm tra trạng thái các tailer đang hoạt động
     */
    @GetMapping("/tailer-status")
    public Map<String, Object> getTailerStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("count", logManagerService.getActiveTailerCount());
        response.put("files", logManagerService.getActiveTailers());
        return response;
    }
}
