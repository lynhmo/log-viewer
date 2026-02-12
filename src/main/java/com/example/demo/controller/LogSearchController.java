package com.example.demo.controller;

import com.example.demo.dto.LogSearchRequest;
import com.example.demo.dto.LogSearchResponse;
import com.example.demo.model.LogEntry;
import com.example.demo.service.LogReaderService;
import com.example.demo.DynamicLogManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogSearchController {

    @Autowired
    private LogReaderService logReaderService;

    @Autowired
    private DynamicLogManagerService dynamicLogManagerService;

    @PostMapping("/search")
    public ResponseEntity<LogSearchResponse> searchLogs(@RequestBody LogSearchRequest request) {
        List<LogEntry> logs = logReaderService.searchLogsByDate(request);
        LogSearchResponse response = new LogSearchResponse(
            logs,
            logs.size(),
            request.getDate()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dates/{serviceName}")
    public ResponseEntity<List<String>> getAvailableLogDates(@PathVariable String serviceName) {
        List<String> dates = logReaderService.getAvailableLogDates(serviceName);
        return ResponseEntity.ok(dates);
    }

    @GetMapping("/services")
    public ResponseEntity<List<String>> getAvailableServices() {
        // Trả về danh sách các service có sẵn
        List<String> services = List.of("order-service", "checkout-service");
        return ResponseEntity.ok(services);
    }

    @PostMapping("/tail/{logId}")
    public ResponseEntity<Map<String, Object>> tailFile(@PathVariable String logId) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Spring tự động decode URL-encoded path parameters, không cần decode manual
            System.out.println("[DEBUG] Tail request for logId: " + logId);
            
            boolean success = dynamicLogManagerService.tailFileOnDemand(logId);
            response.put("success", success);
            response.put("logId", logId);
            
            if (!success) {
                System.err.println("[ERROR] Failed to tail file: " + logId);
                response.put("error", "File not found or cannot be read");
                return ResponseEntity.status(404).body(response);
            }
            
            System.out.println("[DEBUG] Successfully started tailing: " + logId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("[ERROR] Exception in tailFile: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
