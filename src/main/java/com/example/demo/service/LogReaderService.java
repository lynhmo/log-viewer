package com.example.demo.service;

import com.example.demo.dto.LogSearchRequest;
import com.example.demo.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LogReaderService {

    private static final Logger log = LoggerFactory.getLogger(LogReaderService.class);

    @Value("${app.log.base-path:./logs}")
    private String logBasePath;

    private static final DateTimeFormatter LOG_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final DateTimeFormatter FILE_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter TIME_FORMATTER_SECONDS =
        DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER_MINUTES =
        DateTimeFormatter.ofPattern("HH:mm");

    // Pattern để parse log line theo format: yyyy-MM-dd HH:mm:ss.SSS  LEVEL [SERVICE] [traceId=XXX] logger : message
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+" +
        "(\\w+)\\s+" +
        "\\[([^\\]]+)\\]\\s+" +
        "\\[traceId=([^\\]]+)\\]\\s+" +
        "([^:]+)\\s*:\\s*(.*)$"
    );

    public List<LogEntry> searchLogsByDate(LogSearchRequest request) {
        List<LogEntry> allLogs = new ArrayList<>();

        try {
            LocalDate searchDate = LocalDate.parse(request.getDate(), FILE_DATE_FORMATTER);
            String serviceName = request.getServiceName();

            // Compute optional time window
            LocalDateTime fromDateTime = null;
            LocalDateTime toDateTime = null;
            if (request.getStartTime() != null && !request.getStartTime().isBlank()) {
                LocalTime start = parseTimeFlexible(request.getStartTime());
                if (start != null) {
                    fromDateTime = LocalDateTime.of(searchDate, start);
                }
            }
            if (request.getEndTime() != null && !request.getEndTime().isBlank()) {
                LocalTime end = parseTimeFlexible(request.getEndTime());
                if (end != null) {
                    toDateTime = LocalDateTime.of(searchDate, end);
                }
            }

            // Đọc current log file
            Path currentLogPath = Paths.get(logBasePath, serviceName, "app.log");
            log.info("Searching in current log: {}", currentLogPath.toAbsolutePath());

            if (Files.exists(currentLogPath)) {
                allLogs.addAll(readAndFilterLogs(currentLogPath, searchDate, fromDateTime, toDateTime));
            }

            // Đọc archived log files
            Path archivePath = Paths.get(logBasePath, serviceName, "archive");
            log.info("Searching in archive: {}", archivePath.toAbsolutePath());

            if (Files.exists(archivePath) && Files.isDirectory(archivePath)) {
                List<Path> archivedFiles = Files.list(archivePath)
                    .filter(path -> path.getFileName().toString().contains(request.getDate()))
                    .collect(Collectors.toList());

                log.info("Found {} archived files for date: {}", archivedFiles.size(), request.getDate());

                for (Path archivedFile : archivedFiles) {
                    allLogs.addAll(readAndFilterLogs(archivedFile, searchDate, fromDateTime, toDateTime));
                }
            }

            log.info("Total logs found: {}", allLogs.size());

            // Sắp xếp theo thời gian
            allLogs.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));

            // Giới hạn số lượng kết quả
            int limit = request.getLimit() != null ? request.getLimit() : 1000;
            return allLogs.stream().limit(limit).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error reading logs for date: {}", request.getDate(), e);
            return allLogs;
        }
    }

    private List<LogEntry> readAndFilterLogs(Path logFile, LocalDate searchDate, LocalDateTime from, LocalDateTime to) {
        List<LogEntry> logs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LogEntry entry = parseLogLine(line);
                if (entry == null) continue;

                // Filter by date
                if (!entry.getTimestamp().toLocalDate().equals(searchDate)) continue;

                // Optional time window (inclusive)
                if (from != null && entry.getTimestamp().isBefore(from)) continue;
                if (to != null && entry.getTimestamp().isAfter(to)) continue;

                logs.add(entry);
            }
            log.info("Found {} logs in file: {}", logs.size(), logFile.getFileName());
        } catch (IOException e) {
            log.error("Error reading log file: {}", logFile, e);
        }

        return logs;
    }

    private LogEntry parseLogLine(String line) {
        Matcher matcher = LOG_PATTERN.matcher(line);

        if (matcher.matches()) {
            try {
                LogEntry entry = new LogEntry();
                entry.setTimestamp(LocalDateTime.parse(matcher.group(1), LOG_DATE_FORMATTER));
                entry.setLevel(matcher.group(2));
                entry.setServiceName(matcher.group(3));
                entry.setTraceId(matcher.group(4));
                entry.setLogger(matcher.group(5));
                entry.setMessage(matcher.group(6));
                entry.setRawLine(line);
                return entry;
            } catch (Exception e) {
                log.debug("Failed to parse log line: {}", line);
            }
        }

        return null;
    }

    private LocalTime parseTimeFlexible(String timeStr) {
        try {
            if (timeStr.length() == 5) { // HH:mm
                return LocalTime.parse(timeStr, TIME_FORMATTER_MINUTES);
            }
            // HH:mm:ss
            return LocalTime.parse(timeStr, TIME_FORMATTER_SECONDS);
        } catch (Exception e) {
            log.warn("Invalid time format: {} (expected HH:mm or HH:mm:ss)", timeStr);
            return null;
        }
    }

    public List<String> getAvailableLogDates(String serviceName) {
        List<String> dates = new ArrayList<>();

        try {
            // Lấy ngày hiện tại từ current log file
            Path currentLogPath = Paths.get(logBasePath, serviceName, "app.log");
            if (Files.exists(currentLogPath)) {
                dates.add(LocalDate.now().format(FILE_DATE_FORMATTER));
            }

            // Lấy các ngày từ archived files
            Path archivePath = Paths.get(logBasePath, serviceName, "archive");
            if (Files.exists(archivePath) && Files.isDirectory(archivePath)) {
                Files.list(archivePath)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("app-") && name.endsWith(".log"))
                    .map(name -> {
                        // Extract yyyy-MM-dd from app-yyyy-MM-dd.*.log
                        String[] parts = name.split("-");
                        if (parts.length >= 4) {
                            return parts[1] + "-" + parts[2] + "-" + parts[3].split("\\.")[0];
                        }
                        return null;
                    })
                    .filter(date -> date != null)
                    .distinct()
                    .forEach(dates::add);
            }

            // Sắp xếp giảm dần (mới nhất lên đầu)
            dates = dates.stream()
                .distinct()
                .sorted((a, b) -> b.compareTo(a))
                .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("Error listing log dates", e);
        }

        return dates;
    }
}
