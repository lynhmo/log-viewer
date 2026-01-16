# Hướng dẫn cấu hình đường dẫn log theo hệ điều hành

## 📌 Cấu hình mặc định (KHUYÊN DÙNG)

Log được tạo cùng cấp với project folder:
- **Windows:** `E:\CODE\log-viewer\logs\`
- **Linux:** `/path/to/project/logs/`

```yaml
app:
  log:
    base-path: ./logs
```

## Tự động phát hiện OS

Ứng dụng đã được cấu hình để **tự động phát hiện hệ điều hành** (Windows hoặc Linux) và chuẩn hóa đường dẫn file phù hợp.

## Cấu hình trong `application.yml`

### Windows - Cách 1: Đường dẫn tương đối (KHUYÊN DÙNG)
```yaml
app:
  log:
    base-path: ./logs
```
Sẽ tạo folder tại: `E:\CODE\log-viewer\logs\`

### Windows - Cách 2: Đường dẫn tuyệt đối
```yaml
app:
  log:
    base-path: E:/logs
    # Hoặc
    base-path: D:/logs
    # Hoặc
    base-path: C:/logs
```

**Lưu ý:** Trên Windows, bạn có thể dùng cả `/` hoặc `\` làm separator, ứng dụng sẽ tự động chuẩn hóa.

Ví dụ hợp lệ:
- `E:/logs`
- `E:\logs`
- `D:/application/logs`
- `C:\Users\username\logs`

### Linux - Cách 1: Đường dẫn tương đối (KHUYÊN DÙNG)
```yaml
app:
  log:
    base-path: ./logs
```
Sẽ tạo folder tại: `/home/user/project/logs/` (nơi mà ứng dụng chạy)

### Linux - Cách 2: Đường dẫn tuyệt đối
```yaml
app:
  log:
    base-path: /var/log/app
    # Hoặc
    base-path: /home/user/logs
    # Hoặc
    base-path: /opt/app/logs
```

**Lưu ý:** Trên Linux, đường dẫn bắt đầu với `/` và phải có quyền truy cập.

Ví dụ hợp lệ:
- `/var/log/app`
- `/home/user/logs`
- `/opt/myapp/logs`
- `/tmp/logs`

## Cách thức hoạt động

### 1. Phát hiện OS tự động
```java
private static final String OS = System.getProperty("os.name").toLowerCase();
private static final boolean IS_WINDOWS = OS.contains("win");
private static final boolean IS_LINUX = OS.contains("nix") || OS.contains("nux") || OS.contains("aix");
```

### 2. Chuẩn hóa đường dẫn
```java
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
```

### 3. Sử dụng trong code
- **LogViewController**: Tự động chuẩn hóa path khi đọc danh sách file log
- **DynamicLogManagerService**: Tự động chuẩn hóa path khi theo dõi và tail log files

## Kiểm tra OS hiện tại

Khi ứng dụng khởi động, nó sẽ in ra thông tin OS:
```
OS phát hiện: Windows
Thư mục log: E:\CODE\log-viewer\logs
```

Hoặc trên Linux:
```
OS phát hiện: Linux
Thư mục log: /path/to/project/logs
```

## Ví dụ cấu hình đầy đủ

### Windows (application.yml)
```yaml
spring:
  application:
    name: demo1
server:
  port: 8080

logging:
  file:
    path: ./logs
  config: classpath:logback-spring.xml

app:
  log:
    base-path: ./logs
    service-name: order-service
    service-name2: checkout-service
```

**Kết quả:** Log được tạo tại `E:\CODE\log-viewer\logs\`

### Linux (application.yml)
```yaml
spring:
  application:
    name: demo1
server:
  port: 8080

logging:
  file:
    path: ./logs
  config: classpath:logback-spring.xml

app:
  log:
    base-path: ./logs
    service-name: order-service
    service-name2: checkout-service
```

**Kết quả:** Log được tạo tại `/path/to/project/logs/`

## Logback Configuration

File `logback-spring.xml` sử dụng `springProperty` để đọc giá trị từ `application.yml`:

```xml
<springProperty scope="context" name="LOG_BASE" source="app.log.base-path" defaultValue="D:/logs"/>
<springProperty scope="context" name="APP_NAME" source="app.log.service-name" defaultValue="order-service"/>
```

Điều này đảm bảo logback và code Java đều sử dụng cùng một đường dẫn.

## Troubleshooting

### Lỗi: Thư mục không tồn tại
```
Thư mục không tồn tại hoặc không phải thư mục: E:\CODE\log-viewer\logs
```

**Giải pháp:**
Ứng dụng sẽ tự động tạo thư mục nếu nó chưa tồn tại. Nếu lỗi vẫn xảy ra, đảm bảo:
1. Ứng dụng có quyền ghi vào project folder
2. Kiểm tra cấu hình `app.log.base-path` trong `application.yml`

### Lỗi: Permission denied (Linux)
```
java.nio.file.AccessDeniedException: /path/to/project/logs
```

**Giải pháp:**
```bash
# Tạo thư mục và cấp quyền
mkdir -p /path/to/project/logs
chmod 755 /path/to/project/logs
```

### Kiểm tra đường dẫn hiện tại
Xem console log khi ứng dụng khởi động để biết đường dẫn đang được sử dụng:
```
OS phát hiện: Windows
Thư mục log: E:\CODE\log-viewer\logs
```
