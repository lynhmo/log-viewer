package com.example.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.Tailer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
@Service
public class DynamicLogManagerService {

    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.log.base-path}")
    private String logDirectory;

    private final Map<String, Tailer> activeTailers = new ConcurrentHashMap<>();
    private volatile long refreshRateMs = 1000; // Default refresh rate (1 second)

    // Auto-detect OS
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS.contains("win");
    private static final boolean IS_LINUX = OS.contains("nix") || OS.contains("nux") || OS.contains("aix");

    public DynamicLogManagerService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
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

    @PostConstruct
    public void startDirectoryWatcher() {
        // Chuẩn hóa path theo OS
        String normalizedLogDirectory = normalizePath(logDirectory);

        log.info("OS detected: {} | Log directory: {}",
            (IS_WINDOWS ? "Windows" : (IS_LINUX ? "Linux" : "Other")),
            normalizedLogDirectory);

        // 1. Quét các file hiện có lúc khởi động
        Path dirPath = Paths.get(normalizedLogDirectory);
        try {
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                try (Stream<Path> paths = Files.walk(dirPath)) {
                    paths.filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".log"))
                            .map(Path::toFile)
                            .forEach(file -> {
                                try {
                                    startTailing(file);
                                } catch (Exception e) {
                                    log.error("Error tailing file: {}", file.getAbsolutePath(), e);
                                }
                            });
                } catch (Exception e) {
                    log.error("Error scanning directory: {}", normalizedLogDirectory, e);
                }
            } else {
                log.warn("Directory does not exist or is not a directory: {}", normalizedLogDirectory);
            }
        } catch (Exception e) {
            log.error("Error checking directory: {}", normalizedLogDirectory, e);
        }

        // 2. Chạy thread theo dõi thư mục (WatchService) - đệ quy tất cả thư mục con
        Thread watchThread = new Thread(() -> {
            WatchService watcher = null;
            Map<WatchKey, Path> watchKeyPathMap = new ConcurrentHashMap<>();

            try {
                watcher = FileSystems.getDefault().newWatchService();

                // Đăng ký tất cả các thư mục hiện có (đệ quy)
                registerAllDirectories(watcher, Paths.get(normalizedLogDirectory), watchKeyPathMap);

                log.info("Registered {} directories for watching", watchKeyPathMap.size());

                while (true) {
                    WatchKey key;
                    try {
                        key = watcher.take();
                    } catch (InterruptedException e) {
                        log.warn("WatchService thread interrupted");
                        Thread.currentThread().interrupt();
                        break;
                    }

                    Path dir = watchKeyPathMap.get(key);
                    if (dir == null) {
                        log.warn("WatchKey not recognized");
                        continue;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        try {
                            WatchEvent.Kind<?> kind = event.kind();
                            Path fileName = (Path) event.context();
                            Path fullPath = dir.resolve(fileName);
                            File file = fullPath.toFile();

                            // Nếu là thư mục mới được tạo, đăng ký theo dõi nó
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE && file.isDirectory()) {
                                log.info("New directory detected: {}", fullPath);
                                registerAllDirectories(watcher, fullPath, watchKeyPathMap);
                            }

                            // Nếu là file .log, bắt đầu tail
                            if (file.isFile() && fileName.toString().endsWith(".log")) {
                                log.info("New log file detected: {}", fullPath);
                                // Đợi một chút để file được tạo hoàn toàn
                                Thread.sleep(100);
                                if (file.exists() && file.canRead()) {
                                    startTailing(file);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error processing new file event", e);
                        }
                    }

                    if (!key.reset()) {
                        log.warn("WatchKey could not be reset, removing from map");
                        watchKeyPathMap.remove(key);
                    }
                }
            } catch (Exception e) {
                log.error("Fatal error in WatchService", e);
            } finally {
                if (watcher != null) {
                    try {
                        watcher.close();
                    } catch (Exception e) {
                        log.error("Error closing WatchService", e);
                    }
                }
            }
        });
        watchThread.setDaemon(true);
        watchThread.setName("LogDirectoryWatcher");
        watchThread.setUncaughtExceptionHandler((t, e) -> {
            log.error("Uncaught exception in thread: {}", t.getName(), e);
        });
        watchThread.start();
    }

    /**
     * Đăng ký đệ quy tất cả các thư mục con với WatchService
     */
    private void registerAllDirectories(WatchService watcher, Path start, Map<WatchKey, Path> watchKeyPathMap) {
        try (Stream<Path> paths = Files.walk(start)) {
            paths.filter(Files::isDirectory)
                    .forEach(dir -> {
                        try {
                            WatchKey key = dir.register(watcher,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_MODIFY);
                            watchKeyPathMap.put(key, dir);
                            log.info("Registering watch for: {}", dir);
                        } catch (Exception e) {
                            log.error("Cannot register directory: {}", dir, e);
                        }
                    });
        } catch (Exception e) {
            log.error("Error registering directories from: {}", start, e);
        }
    }


    private void startTailing(File file) {
        try {
            // Chuẩn hóa path và dùng đường dẫn tương đối từ logDirectory làm ID để tránh trùng lặp
            String normalizedLogDirectory = normalizePath(logDirectory);
            String logId = Paths.get(normalizedLogDirectory).relativize(file.toPath()).toString().replace("\\", "/");

            if (activeTailers.containsKey(logId)) {
                log.debug("File already being tailed: {}", logId);
                return;
            }

            if (!file.exists() || !file.canRead()) {
                log.warn("File does not exist or cannot be read: {}", file.getAbsolutePath());
                return;
            }

            log.info("Start tailing file: {} ({})", logId, file.getAbsolutePath());

            LogTailerListener listener = new LogTailerListener(messagingTemplate, logId);
            Tailer tailer = new Tailer(file, listener, refreshRateMs, true);

            Thread thread = new Thread(tailer);
            thread.setDaemon(true);
            thread.setName("Tailer-" + logId);
            thread.setUncaughtExceptionHandler((t, e) -> {
                log.error("Error in tailer thread: {}", t.getName(), e);
                activeTailers.remove(logId);
            });
            thread.start();

            activeTailers.put(logId, tailer);

            // Notify FE about new file (if needed)
            messagingTemplate.convertAndSend("/topic/new-file", logId);
        } catch (Exception e) {
            log.error("Error initializing tailer for file: {}", file.getAbsolutePath(), e);
        }
    }

    /**
     * Lấy danh sách các file đang được tail
     */
    public Map<String, String> getActiveTailers() {
        Map<String, String> result = new ConcurrentHashMap<>();
        activeTailers.forEach((logId, tailer) -> {
            result.put(logId, "Running");
        });
        return result;
    }

    /**
     * Lấy số lượng tailer đang hoạt động
     */
    public int getActiveTailerCount() {
        return activeTailers.size();
    }

    /**
     * Lấy đường dẫn thư mục log
     */
    public String getLogDirectory() {
        return logDirectory;
    }

    /**
     * Cập nhật tốc độ làm mới cho tất cả các tailer
     * LƯU Ý: Tailer không hỗ trợ thay đổi delay động, nên cần restart
     */
    public synchronized void updateRefreshRate(long newRateMs) {
        if (newRateMs < 100 || newRateMs > 10000) {
            log.warn("Invalid refresh rate: {}ms (must be between 100-10000ms)", newRateMs);
            return;
        }

        this.refreshRateMs = newRateMs;
        log.info("Refresh rate updated: {}ms", newRateMs);

        // Restart tất cả các tailer với delay mới
        restartAllTailers();
    }

    /**
     * Restart tất cả các tailer với refresh rate mới
     */
    private void restartAllTailers() {
        log.info("Restarting {} tailer(s)...", activeTailers.size());

        // Chuẩn hóa path
        String normalizedLogDirectory = normalizePath(logDirectory);

        // Lưu lại danh sách file đang tail
        Map<String, File> filesToRestart = new ConcurrentHashMap<>();
        activeTailers.forEach((logId, tailer) -> {
            try {
                // Dừng tailer cũ
                tailer.stop();

                // Tìm lại file từ logId
                Path filePath = Paths.get(normalizedLogDirectory).resolve(logId);
                File file = filePath.toFile();
                if (file.exists() && file.canRead()) {
                    filesToRestart.put(logId, file);
                }
            } catch (Exception e) {
                log.error("Error stopping tailer: {}", logId, e);
            }
        });

        // Xóa các tailer cũ
        activeTailers.clear();

        // Tạo lại các tailer với delay mới
        filesToRestart.forEach((logId, file) -> {
            try {
                Thread.sleep(50); // Đợi một chút giữa các restart
                startTailingWithoutNotification(file);
            } catch (Exception e) {
                log.error("Error restarting tailer: {}", logId, e);
            }
        });

        log.info("Restarted {} tailer(s)", activeTailers.size());
    }

    /**
     * Bắt đầu tail file nhưng không gửi thông báo new-file (dùng cho restart)
     */
    private void startTailingWithoutNotification(File file) {
        try {
            String normalizedLogDirectory = normalizePath(logDirectory);
            String logId = Paths.get(normalizedLogDirectory).relativize(file.toPath()).toString().replace("\\", "/");

            if (activeTailers.containsKey(logId)) {
                return;
            }

            if (!file.exists() || !file.canRead()) {
                log.warn("File does not exist or cannot be read: {}", file.getAbsolutePath());
                return;
            }

            log.info("Restart tail file: {} with refresh rate {}ms", logId, refreshRateMs);

            LogTailerListener listener = new LogTailerListener(messagingTemplate, logId);
            Tailer tailer = new Tailer(file, listener, refreshRateMs, true);

            Thread thread = new Thread(tailer);
            thread.setDaemon(true);
            thread.setName("Tailer-" + logId);
            thread.setUncaughtExceptionHandler((t, e) -> {
                log.error("Error in tailer thread: {}", t.getName(), e);
                activeTailers.remove(logId);
            });
            thread.start();

            activeTailers.put(logId, tailer);
        } catch (Exception e) {
            log.error("Error restarting tailer for file: {}", file.getAbsolutePath(), e);
        }
    }
}