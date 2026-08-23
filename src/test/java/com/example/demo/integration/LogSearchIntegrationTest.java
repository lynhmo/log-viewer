package com.example.demo.integration;

import com.example.demo.dto.LogSearchRequest;
import com.example.demo.dto.LogSearchResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LogSearchIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static Path tempLogDir;

    private static final String SERVICE_NAME = "order-service";

    private static final String SAMPLE_LOG_LINE =
        "2026-06-15 10:30:45.123  INFO [order-service] [traceId=abc12345] com.example.demo.TestClass : Order created successfully\n";
    private static final String SAMPLE_LOG_LINE_ERROR =
        "2026-06-15 10:30:46.323 ERROR [order-service] [traceId=def67890] com.example.demo.TestClass : Out of stock exception\n";

    @BeforeAll
    static void setUp() throws IOException {
        // Override app.log.base-path via system property
        tempLogDir = Files.createTempDirectory("logsee-int-test-");
        System.setProperty("app.log.base-path", tempLogDir.toString());

        // Create service directory and log files
        Path serviceDir = tempLogDir.resolve(SERVICE_NAME);
        Files.createDirectories(serviceDir);

        // Current log file
        Path currentLog = serviceDir.resolve("app.log");
        Files.writeString(currentLog, SAMPLE_LOG_LINE + SAMPLE_LOG_LINE_ERROR);

        // Archive directory with archived log
        Path archiveDir = serviceDir.resolve("archive");
        Files.createDirectories(archiveDir);
        Path archiveLog = archiveDir.resolve("app-2026-06-14.1.log");
        Files.writeString(archiveLog, "2026-06-14 09:00:00.000  INFO [order-service] [traceId=old123] com.example.demo.TestClass : Old log entry\n");

        // Second service
        Path serviceDir2 = tempLogDir.resolve("checkout-service");
        Files.createDirectories(serviceDir2);
    }

    @Test
    void shouldReturnLogs_whenSearchingExistingDate() {
        LogSearchRequest request = new LogSearchRequest();
        request.setDate("2026-06-15");
        request.setServiceName(SERVICE_NAME);

        ResponseEntity<LogSearchResponse> response = restTemplate.postForEntity(
            "/api/logs/search", request, LogSearchResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLogs()).hasSize(2);
        assertThat(response.getBody().getTotalCount()).isEqualTo(2);
        assertThat(response.getBody().getSearchDate()).isEqualTo("2026-06-15");
    }

    @Test
    void shouldFilterByTimeRange_whenStartEndTimeProvided() {
        LogSearchRequest request = new LogSearchRequest();
        request.setDate("2026-06-15");
        request.setServiceName(SERVICE_NAME);
        request.setStartTime("10:30:46");

        ResponseEntity<LogSearchResponse> response = restTemplate.postForEntity(
            "/api/logs/search", request, LogSearchResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLogs()).hasSize(1);
        assertThat(response.getBody().getLogs().get(0).getLevel()).isEqualTo("ERROR");
    }

    @Test
    void shouldReturnAvailableDates() {
        ResponseEntity<List<String>> response = restTemplate.exchange(
            "/api/logs/dates/" + SERVICE_NAME,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<String>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).anyMatch(d -> d.equals("2026-06-14"));
    }

    @Test
    void shouldReturnServices() {
        ResponseEntity<List<String>> response = restTemplate.exchange(
            "/api/logs/services",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<String>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("checkout-service");
    }

    @Test
    void shouldReturnEmpty_whenDateInvalid() {
        LogSearchRequest request = new LogSearchRequest();
        request.setDate("not-a-date");
        request.setServiceName(SERVICE_NAME);

        ResponseEntity<LogSearchResponse> response = restTemplate.postForEntity(
            "/api/logs/search", request, LogSearchResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLogs()).isEmpty();
    }
}
