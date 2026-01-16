package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * Endpoint để lấy danh sách các ngày có log archive
     * @param logFile - File log hiện tại (VD: order-service/app.log)
     * @return Danh sách các ngày có log cũ
     */
    @GetMapping("/available-dates")
    public Map<String, Object> getAvailableDates(@RequestParam String logFile) {
        Map<String, Object> response = new HashMap<>();
        List<String> dates = new ArrayList<>();

        try {
            // Lấy thư mục archive
            String logDirectory = logManagerService.getLogDirectory();
            String serviceName = logFile.substring(0, logFile.lastIndexOf('/'));
            Path archiveDir = Paths.get(logDirectory)
                    .resolve(serviceName)
                    .resolve("archive");

            if (Files.exists(archiveDir) && Files.isDirectory(archiveDir)) {
                // Tìm tất cả file .log và extract ngày từ tên file
                // Format: app-YYYY-MM-DD.1.log
                dates = Files.list(archiveDir)
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".log"))
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .filter(name -> name.startsWith("app-") && name.contains("-"))
                        .map(name -> {
                            // Extract YYYY-MM-DD from app-YYYY-MM-DD.1.log
                            try {
                                String dateStr = name.substring(4); // Remove "app-"
                                return dateStr.substring(0, 10); // Get YYYY-MM-DD
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(date -> date != null)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
            }

            response.put("success", true);
            response.put("logFile", logFile);
            response.put("dates", dates);

        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error reading archive directory: " + e.getMessage());
        }

        return response;
    }

    /**
     * Endpoint để lấy log cũ theo ngày tháng và số dòng
     * @param logFile - Đường dẫn file log (VD: order-service/archive/app-2026-01-17.1.log)
     * @param lineCount - Số dòng log cần lấy (mặc định 100)
     * @return Danh sách dòng log
     */
    @GetMapping("/historical-logs")
    public Map<String, Object> getHistoricalLogs(
            @RequestParam String logFile,
            @RequestParam(defaultValue = "100") int lineCount) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Chuẩn hóa path
            String logDirectory = logManagerService.getLogDirectory();
            Path filePath = Paths.get(logDirectory).resolve(logFile);

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                response.put("success", false);
                response.put("message", "File not found: " + logFile);
                return response;
            }

            // Đọc tất cả dòng từ file
            List<String> allLines = Files.readAllLines(filePath);

            // Lấy N dòng cuối cùng (hoặc ít hơn nếu file có ít dòng hơn)
            int startIndex = Math.max(0, allLines.size() - lineCount);
            List<String> lines = allLines.subList(startIndex, allLines.size());

            response.put("success", true);
            response.put("file", logFile);
            response.put("totalLines", allLines.size());
            response.put("loadedLines", lines.size());
            response.put("lines", lines);

        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error reading file: " + e.getMessage());
        }

        return response;
    }
}
