package com.example.demo;

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

    private static final String LOG_DIRECTORY = "D:/logs/";

    @GetMapping("/logs")
    public String viewLogs(Model model) {
        List<String> logFiles = new ArrayList<>();

        try {
            Path dirPath = Paths.get(LOG_DIRECTORY);
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                try (Stream<Path> paths = Files.walk(dirPath)) {
                    paths.filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".log"))
                            .forEach(path -> {
                                // Lấy đường dẫn tương đối từ LOG_DIRECTORY
                                String relativePath = dirPath.relativize(path).toString().replace("\\", "/");
                                logFiles.add(relativePath);
                            });
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi quét thư mục log: " + e.getMessage());
            e.printStackTrace();
        }

        model.addAttribute("files", logFiles);
        return "log-monitor";
    }
}