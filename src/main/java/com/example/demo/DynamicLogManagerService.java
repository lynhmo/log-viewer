package com.example.demo;

import org.apache.commons.io.input.Tailer;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
public class DynamicLogManagerService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final Map<String, Tailer> activeTailers = new ConcurrentHashMap<>();
    private final String LOG_DIRECTORY = "D:/logs/"; // Thư mục chứa log
    private volatile long refreshRateMs = 1000; // Tốc độ làm mới mặc định (1 giây)

    @PostConstruct
    public void startDirectoryWatcher() {
        // 1. Quét các file hiện có lúc khởi động
        Path dirPath = Paths.get(LOG_DIRECTORY);
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
                                    System.err.println("Lỗi khi tail file: " + file.getAbsolutePath());
                                    e.printStackTrace();
                                }
                            });
                } catch (Exception e) {
                    System.err.println("Lỗi khi quét thư mục: " + LOG_DIRECTORY);
                    e.printStackTrace();
                }
            } else {
                System.err.println("Thư mục không tồn tại hoặc không phải thư mục: " + LOG_DIRECTORY);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi kiểm tra thư mục: " + LOG_DIRECTORY);
            e.printStackTrace();
        }

        // 2. Chạy thread theo dõi thư mục (WatchService) - đệ quy tất cả thư mục con
        Thread watchThread = new Thread(() -> {
            WatchService watcher = null;
            Map<WatchKey, Path> watchKeyPathMap = new ConcurrentHashMap<>();

            try {
                watcher = FileSystems.getDefault().newWatchService();

                // Đăng ký tất cả các thư mục hiện có (đệ quy)
                registerAllDirectories(watcher, Paths.get(LOG_DIRECTORY), watchKeyPathMap);

                System.out.println("Đã đăng ký theo dõi " + watchKeyPathMap.size() + " thư mục");

                while (true) {
                    WatchKey key;
                    try {
                        key = watcher.take();
                    } catch (InterruptedException e) {
                        System.err.println("Thread WatchService bị ngắt");
                        Thread.currentThread().interrupt();
                        break;
                    }

                    Path dir = watchKeyPathMap.get(key);
                    if (dir == null) {
                        System.err.println("WatchKey không được nhận diện!");
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
                                System.out.println("Phát hiện thư mục mới: " + fullPath);
                                registerAllDirectories(watcher, fullPath, watchKeyPathMap);
                            }

                            // Nếu là file .log, bắt đầu tail
                            if (file.isFile() && fileName.toString().endsWith(".log")) {
                                System.out.println("Phát hiện file log mới: " + fullPath);
                                // Đợi một chút để file được tạo hoàn toàn
                                Thread.sleep(100);
                                if (file.exists() && file.canRead()) {
                                    startTailing(file);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi khi xử lý sự kiện file mới");
                            e.printStackTrace();
                        }
                    }

                    if (!key.reset()) {
                        System.err.println("WatchKey không thể reset, gỡ bỏ khỏi map");
                        watchKeyPathMap.remove(key);
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi nghiêm trọng trong WatchService");
                e.printStackTrace();
            } finally {
                if (watcher != null) {
                    try {
                        watcher.close();
                    } catch (Exception e) {
                        System.err.println("Lỗi khi đóng WatchService");
                        e.printStackTrace();
                    }
                }
            }
        });
        watchThread.setDaemon(true);
        watchThread.setName("LogDirectoryWatcher");
        watchThread.setUncaughtExceptionHandler((t, e) -> {
            System.err.println("Exception không xử lý được trong thread: " + t.getName());
            e.printStackTrace();
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
                            System.out.println("Đăng ký theo dõi: " + dir);
                        } catch (Exception e) {
                            System.err.println("Không thể đăng ký thư mục: " + dir);
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            System.err.println("Lỗi khi đăng ký thư mục: " + start);
            e.printStackTrace();
        }
    }


    private void startTailing(File file) {
        try {
            // Dùng đường dẫn tương đối từ LOG_DIRECTORY làm ID để tránh trùng lặp
            String logId = Paths.get(LOG_DIRECTORY).relativize(file.toPath()).toString().replace("\\", "/");

            if (activeTailers.containsKey(logId)) {
                System.out.println("File đã được tail: " + logId);
                return;
            }

            if (!file.exists() || !file.canRead()) {
                System.err.println("File không tồn tại hoặc không thể đọc: " + file.getAbsolutePath());
                return;
            }

            System.out.println("Bắt đầu tail file: " + logId + " (" + file.getAbsolutePath() + ")");

            LogTailerListener listener = new LogTailerListener(messagingTemplate, logId);
            Tailer tailer = new Tailer(file, listener, refreshRateMs, true);

            Thread thread = new Thread(tailer);
            thread.setDaemon(true);
            thread.setName("Tailer-" + logId);
            thread.setUncaughtExceptionHandler((t, e) -> {
                System.err.println("Lỗi trong thread tailer: " + t.getName());
                e.printStackTrace();
                activeTailers.remove(logId);
            });
            thread.start();

            activeTailers.put(logId, tailer);

            // Thông báo cho FE biết có file mới (nếu cần)
            messagingTemplate.convertAndSend("/topic/new-file", logId);
        } catch (Exception e) {
            System.err.println("Lỗi khi khởi tạo tailer cho file: " + file.getAbsolutePath());
            e.printStackTrace();
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
     * Cập nhật tốc độ làm mới cho tất cả các tailer
     * LƯU Ý: Tailer không hỗ trợ thay đổi delay động, nên cần restart
     */
    public synchronized void updateRefreshRate(long newRateMs) {
        if (newRateMs < 100 || newRateMs > 10000) {
            System.err.println("Refresh rate không hợp lệ: " + newRateMs + "ms (phải từ 100-10000ms)");
            return;
        }

        this.refreshRateMs = newRateMs;
        System.out.println("Đã cập nhật refresh rate: " + newRateMs + "ms");

        // Restart tất cả các tailer với delay mới
        restartAllTailers();
    }

    /**
     * Restart tất cả các tailer với refresh rate mới
     */
    private void restartAllTailers() {
        System.out.println("Đang restart " + activeTailers.size() + " tailer(s)...");

        // Lưu lại danh sách file đang tail
        Map<String, File> filesToRestart = new ConcurrentHashMap<>();
        activeTailers.forEach((logId, tailer) -> {
            try {
                // Dừng tailer cũ
                tailer.stop();

                // Tìm lại file từ logId
                Path filePath = Paths.get(LOG_DIRECTORY).resolve(logId);
                File file = filePath.toFile();
                if (file.exists() && file.canRead()) {
                    filesToRestart.put(logId, file);
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi dừng tailer: " + logId);
                e.printStackTrace();
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
                System.err.println("Lỗi khi restart tailer: " + logId);
                e.printStackTrace();
            }
        });

        System.out.println("Đã restart xong " + activeTailers.size() + " tailer(s)");
    }

    /**
     * Bắt đầu tail file nhưng không gửi thông báo new-file (dùng cho restart)
     */
    private void startTailingWithoutNotification(File file) {
        try {
            String logId = Paths.get(LOG_DIRECTORY).relativize(file.toPath()).toString().replace("\\", "/");

            if (activeTailers.containsKey(logId)) {
                return;
            }

            if (!file.exists() || !file.canRead()) {
                System.err.println("File không tồn tại hoặc không thể đọc: " + file.getAbsolutePath());
                return;
            }

            System.out.println("Restart tail file: " + logId + " với refresh rate " + refreshRateMs + "ms");

            LogTailerListener listener = new LogTailerListener(messagingTemplate, logId);
            Tailer tailer = new Tailer(file, listener, refreshRateMs, true);

            Thread thread = new Thread(tailer);
            thread.setDaemon(true);
            thread.setName("Tailer-" + logId);
            thread.setUncaughtExceptionHandler((t, e) -> {
                System.err.println("Lỗi trong thread tailer: " + t.getName());
                e.printStackTrace();
                activeTailers.remove(logId);
            });
            thread.start();

            activeTailers.put(logId, tailer);
        } catch (Exception e) {
            System.err.println("Lỗi khi restart tailer cho file: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }
}