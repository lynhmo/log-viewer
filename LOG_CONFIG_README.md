# Log Viewer - Cấu hình Log

## 📁 Cấu trúc thư mục sau khi chạy

```
log-viewer/
├── logs/                  ← Log files được tạo tại đây
│   ├── order-service/
│   │   ├── app.log
│   │   └── archive/
│   ├── checkout-service/
│   │   ├── app.log
│   │   └── archive/
├── src/
├── pom.xml
├── application.yml
└── ...
```

## ⚙️ Cấu hình mặc định

**File:** `src/main/resources/application.yml`

```yaml
app:
  log:
    base-path: ./logs
    service-name: order-service
    service-name2: checkout-service
```

## 🚀 Cách hoạt động

### 1. **Tự động phát hiện OS**
- **Windows:** Log được lưu tại `E:\CODE\log-viewer\logs\`
- **Linux:** Log được lưu tại `/path/to/project/logs/`

### 2. **Tạo thư mục tự động**
- Nếu thư mục `logs` chưa tồn tại, ứng dụng sẽ tự động tạo

### 3. **Quản lý nhiều service**
- Log của `order-service` → `logs/order-service/app.log`
- Log của `checkout-service` → `logs/checkout-service/app.log`

### 4. **Xoay vòng log (Rotation)**
- Log file tối đa 50MB
- Lưu giữ 14 ngày log cũ
- Tự động archive log cũ vào thư mục `archive/`

## 📝 Thay đổi cấu hình

### Đặt log ở vị trí khác (Windows)
```yaml
app:
  log:
    base-path: E:/mylogs
```

### Đặt log ở vị trí khác (Linux)
```yaml
app:
  log:
    base-path: /var/log/myapp
```

## 🔧 Logback Configuration

**File:** `src/main/resources/logback-spring.xml`

- Đọc cấu hình từ `application.yml`
- Hỗ trợ cả Console và File appender
- Tự động rotate log theo ngày và kích thước

## ✅ Lợi ích

✅ Log được tạo cùng cấp với project (dễ backup)  
✅ Tự động phát hiện OS (Windows/Linux)  
✅ Không cần tạo thư mục thủ công  
✅ Quản lý nhiều service log riêng biệt  
✅ Tự động xoay vòng và archive log cũ  
✅ Dễ dàng thay đổi vị trí log qua config  

## 📚 Xem thêm

- `OS_PATH_CONFIG_GUIDE.md` - Hướng dẫn chi tiết cấu hình theo OS
- `USER_GUIDE.md` - Hướng dẫn sử dụng ứng dụng
- `logback-spring.xml` - Cấu hình chi tiết của Logback
