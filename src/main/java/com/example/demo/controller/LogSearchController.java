package com.example.demo.controller;

import com.example.demo.dto.LogSearchRequest;
import com.example.demo.dto.LogSearchResponse;
import com.example.demo.model.LogEntry;
import com.example.demo.service.LogReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogSearchController {

    @Autowired
    private LogReaderService logReaderService;

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
}
