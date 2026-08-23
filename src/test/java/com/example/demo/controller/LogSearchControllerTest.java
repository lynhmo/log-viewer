package com.example.demo.controller;

import com.example.demo.dto.LogSearchRequest;
import com.example.demo.dto.LogSearchResponse;
import com.example.demo.model.LogEntry;
import com.example.demo.service.LogReaderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@WebMvcTest(LogSearchController.class)
class LogSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LogReaderService logReaderService;

    private LogEntry createSampleLogEntry(String level, String message) {
        return new LogEntry(
            LocalDateTime.of(2026, 6, 15, 10, 30, 45),
            level, "order-service", "trace123",
            "com.example.TestClass", message,
            "2026-06-15 10:30:45.123  " + level + " [order-service] [traceId=trace123] com.example.TestClass : " + message
        );
    }

    @Test
    void shouldReturn200WithLogEntries_whenSearchIsValid() throws Exception {
        List<LogEntry> mockLogs = List.of(
            createSampleLogEntry("INFO", "Order created"),
            createSampleLogEntry("ERROR", "Out of stock")
        );
        when(logReaderService.searchLogsByDate(any(LogSearchRequest.class))).thenReturn(mockLogs);

        LogSearchRequest request = new LogSearchRequest();
        request.setDate("2026-06-15");
        request.setServiceName("order-service");

        mockMvc.perform(post("/api/logs/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.logs").isArray())
            .andExpect(jsonPath("$.logs.length()").value(2))
            .andExpect(jsonPath("$.totalCount").value(2))
            .andExpect(jsonPath("$.searchDate").value("2026-06-15"));
    }

    @Test
    void shouldReturn500_whenServiceThrowsUnexpectedException() throws Exception {
        when(logReaderService.searchLogsByDate(any(LogSearchRequest.class)))
            .thenThrow(new RuntimeException("Unexpected error"));

        LogSearchRequest request = new LogSearchRequest();
        request.setDate("2026-06-15");
        request.setServiceName("order-service");

        mockMvc.perform(post("/api/logs/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void shouldReturn200WithDates_whenServiceExists() throws Exception {
        when(logReaderService.getAvailableLogDates("order-service"))
            .thenReturn(List.of("2026-06-15", "2026-06-14"));

        mockMvc.perform(get("/api/logs/dates/order-service"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturn200WithEmptyDates_whenServiceNotFound() throws Exception {
        when(logReaderService.getAvailableLogDates("non-existent"))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/logs/dates/non-existent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn200WithServiceList_whenGetServices() throws Exception {
        mockMvc.perform(get("/api/logs/services"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0]").value("order-service"))
            .andExpect(jsonPath("$[1]").value("checkout-service"));
    }
}
