package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Controller
public class LogViewController {

    @Value("${app.log.base-path}")
    private String logDirectory;

    // Tự động phát hiện OS
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS.contains("win");
    private static final boolean IS_LINUX = OS.contains("nix") || OS.contains("nux") || OS.contains("aix");

    @GetMapping("/logs")
    public String viewLogs(Model model) {
        List<String> logFiles = new ArrayList<>();

        try {
            // Chuẩn hóa path theo OS
            String normalizedPath = normalizePath(logDirectory);
            Path dirPath = Paths.get(normalizedPath);

            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                try (Stream<Path> paths = Files.walk(dirPath)) {
                    paths.filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".log"))
                            .forEach(path -> {
                                // Lấy đường dẫn tương đối và chuẩn hóa separator thành "/"
                                String relativePath = dirPath.relativize(path).toString()
                                        .replace(File.separator, "/");
                                logFiles.add(relativePath);
                            });
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi quét thư mục log: " + e.getMessage());
            e.printStackTrace();
        }

        model.addAttribute("files", logFiles);
        model.addAttribute("os", IS_WINDOWS ? "Windows" : (IS_LINUX ? "Linux" : "Other"));
        return "log-monitor";
    }

    @GetMapping("/test-search")
    public String testSearch() {
        return "test-log-search";
    }

    /**
     * Chuẩn hóa path theo hệ điều hành
     */
    private String normalizePath(String path) {
        if (path == null) {
            return IS_WINDOWS ? "C:/logs" : "/var/log/app";
        }

        // Thay thế separator cho đúng OS
        if (IS_WINDOWS) {
            return path.replace("/", File.separator);
        } else {
            return path.replace("\\", File.separator);
        }
    }
}