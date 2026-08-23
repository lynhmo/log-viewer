package com.example.demo.service;

import com.example.demo.dto.LogSearchRequest;
import com.example.demo.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class LogReaderServiceTest {

    private LogReaderService service;

    private Path tempLogDir;

    private static final String SAMPLE_LOG_LINE =
        "2026-06-15 10:30:45.123  INFO [order-service] [traceId=abc12345] com.example.demo.TestClass : Order created successfully\n";
    private static final String SAMPLE_LOG_LINE_WARN =
        "2026-06-15 10:30:46.223  WARN [order-service] [traceId=def67890] com.example.demo.TestClass : Slow response detected\n";
    private static final String SAMPLE_LOG_LINE_ERROR =
        "2026-06-15 10:30:47.323 ERROR [order-service] [traceId=ghi11111] com.example.demo.TestClass : Out of stock exception\n";
    private static final String SAMPLE_LOG_LINE_DIFF_DATE =
        "2026-06-16 10:30:45.123  INFO [order-service] [traceId=xyz99999] com.example.demo.TestClass : Order created\n";

    @BeforeEach
    void setUp() throws IOException {
        service = new LogReaderService();
        tempLogDir = Files.createTempDirectory("logsee-test-");
        ReflectionTestUtils.setField(service, "logBasePath", tempLogDir.toString());
    }

    private Path createServiceDir(String serviceName) throws IOException {
        Path serviceDir = tempLogDir.resolve(serviceName);
        Files.createDirectories(serviceDir);
        return serviceDir;
    }

    private Path createArchiveDir(String serviceName) throws IOException {
        Path archiveDir = tempLogDir.resolve(serviceName).resolve("archive");
        Files.createDirectories(archiveDir);
        return archiveDir;
    }

    private void writeLogFile(Path file, String... lines) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
        }
        Files.writeString(file, sb.toString());
    }

    // ─── LogSearchRequest.searchLogsByDate() ───

    @Test
    void shouldReturnLogs_whenSearchingInCurrentFile() throws IOException {
        Path serviceDir = createServiceDir("order-service");
        Path logFile = serviceDir.resolve("app.log");
        writeLogFile(logFile, SAMPLE_LOG_LINE, SAMPLE_LOG_LINE_WARN, SAMPLE_LOG_LINE_ERROR);

        LogSearchRequest request = new LogSearchRequest();
        request.setDate("2026-06-15");
        request.setServiceName("order-service");

        List<LogEntry> result = service.searchLogsByDate(request);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getLevel()).isEqualTo("INFO");
        assertThat(result.get(1).getLevel()).isEqualTo("WARN");
        assertThat(result.get(2).getLevel()).isEqualTo("ERROR");
    }

    @Test
    void shouldReturnEmptyList_whenNoLogsMatchDate() throws IOException {
        Path serviceDir = createServiceDir("order-service");
        Path logFile = serviceDir.resolve("app.log");
        writeLogFile(logFile, SAMPLE_LOG_LINE);

        LogSearchRequest request = new LogSearchRequest();
        request.setDate("2026-12-25");
        request.setServiceName("order-service");

        List<LogEntry> result = service.searchLogsByDate(request);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyList_whenDateFormatIsInvalid() throws IOException {
        Path serviceDir = createServiceDir("order-service");
        Path logFile = serviceDir.resolve("app.log");
        writeLogFile(logFile, SAMPLE_LOG_LINE);

        LogSearchRequest request = new LogSearchRequest();
        request.setDate("not-a-date");
        request.setServiceName("order-service");

        List<LogEntry> result = service.searchLogsByDate(request);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldFilterLogsByTimeRange_whenStartAndEndTimeProvided() throws IOException {
        Path serviceDir = createServiceDir("order-service");
        Path logFile = serviceDir.resolve("app.log");
        writeLogFile(logFile, SAMPLE_LOG_LINE, SAMPLE_LOG_LINE_WARN, SAMPLE_LOG_LINE_ERROR);

        LogSearchRequest request = new LogSearchRequest();
        request.setDate("2026-06-15");
        request.setServiceName("order-service");
        request.setStartTime("10:30:46");
        request.setEndTime("10:30:47");

        List<LogEntry> result = service.searchLogsByDate(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLevel()).isEqualTo("WARN");
    }

    @Test
    void shouldIncludeArchivedLogs_whenArchiveFileMatchesDate() throws IOException {
        Path serviceDir = createServiceDir("order-service");
        Path logFile = serviceDir.resolve("app.log");
        writeLogFile(logFile, SAMPLE_LOG_LINE);

        // Create archive file with matching date
        Path archiveDir = createArchiveDir("order-service");
        Path archiveFile = archiveDir.resolve("app-2026-06-15.1.log");
        writeLogFile(archiveFile, SAMPLE_LOG_LINE_WARN, SAMPLE_LOG_LINE_ERROR);

        LogSearchRequest request = new LogSearchRequest();
        request.setDate("2026-06-15");
        request.setServiceName("order-service");

        List<LogEntry> result = service.searchLogsByDate(request);

        assertThat(result).hasSize(3);
    }

    @Test
    void shouldReturnEmptyList_whenLogFileDoesNotExist() {
        LogSearchRequest request = new LogSearchRequest();
        request.setDate("2026-06-15");
        request.setServiceName("non-existent-service");

        List<LogEntry> result = service.searchLogsByDate(request);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldRespectLimit_whenMoreLogsThanLimitExist() throws IOException {
        Path serviceDir = createServiceDir("order-service");
        Path logFile = serviceDir.resolve("app.log");
        StringBuilder manyLines = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            manyLines.append(SAMPLE_LOG_LINE);
        }
        Files.writeString(logFile, manyLines.toString());

        LogSearchRequest request = new LogSearchRequest();
        request.setDate("2026-06-15");
        request.setServiceName("order-service");
        request.setLimit(10);

        List<LogEntry> result = service.searchLogsByDate(request);

        assertThat(result).hasSize(10);
    }

    @Test
    void shouldFilterFromStartTime_whenOnlyStartTimeProvided() throws IOException {
        Path serviceDir = createServiceDir("order-service");
        Path logFile = serviceDir.resolve("app.log");
        writeLogFile(logFile, SAMPLE_LOG_LINE, SAMPLE_LOG_LINE_WARN, SAMPLE_LOG_LINE_ERROR);

        LogSearchRequest request = new LogSearchRequest();
        request.setDate("2026-06-15");
        request.setServiceName("order-service");
        request.setStartTime("10:30:47");

        List<LogEntry> result = service.searchLogsByDate(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLevel()).isEqualTo("ERROR");
    }

    // ─── LogSearchRequest.getAvailableLogDates() ───

    @Test
    void shouldReturnDatesFromCurrentAndArchiveLogs() throws IOException {
        Path serviceDir = createServiceDir("order-service");
        Path logFile = serviceDir.resolve("app.log");
        writeLogFile(logFile, SAMPLE_LOG_LINE);

        Path archiveDir = createArchiveDir("order-service");
        Path archiveFile1 = archiveDir.resolve("app-2026-06-14.1.log");
        Path archiveFile2 = archiveDir.resolve("app-2026-06-15.1.log");
        Files.writeString(archiveFile1, SAMPLE_LOG_LINE);
        Files.writeString(archiveFile2, SAMPLE_LOG_LINE_ERROR);

        List<String> dates = service.getAvailableLogDates("order-service");

        assertThat(dates)
            .isNotEmpty()
            .anyMatch(d -> d.equals("2026-06-14"))
            .anyMatch(d -> d.equals("2026-06-15"));
    }

    @Test
    void shouldReturnEmptyList_whenServiceDirectoryDoesNotExist() {
        List<String> dates = service.getAvailableLogDates("non-existent-service");

        assertThat(dates).isEmpty();
    }
}
