# Realtime Log Monitor - User Guide

## 🎯 Tổng quan

Ứng dụng theo dõi log file realtime với khả năng:
- ✅ Quét đệ quy tất cả thư mục con (nhiều cấp)
- ✅ Tự động phát hiện file log mới
- ✅ **Điều chỉnh tốc độ làm mới (Refresh Rate)** ⭐ MỚI
- ✅ WebSocket streaming realtime
- ✅ UI thân thiện với color highlighting

---

## 🚀 Khởi động nhanh

### 1. Chạy ứng dụng
```bash
.\mvnw.cmd spring-boot:run
```

### 2. Truy cập giao diện
```
http://localhost:8080/logs
```

### 3. Tạo folder và file log test
```powershell
# Tạo cấu trúc thư mục
New-Item -ItemType Directory -Path "D:\logs\level1\level2\level3" -Force

# Tạo file log
"Test log root" | Out-File "D:\logs\app.log" -Encoding UTF8
"Test log level1" | Out-File "D:\logs\level1\service.log" -Encoding UTF8
"Test log level2" | Out-File "D:\logs\level1\level2\database.log" -Encoding UTF8

# Thêm dòng mới
"New log line" | Out-File "D:\logs\app.log" -Append -Encoding UTF8
```

---

## ⚙️ Tính năng Refresh Rate ⭐ MỚI

### Cách sử dụng:

1. **Tìm slider Refresh Rate** ở góc trên bên phải giao diện
2. **Kéo slider** để chọn tốc độ (100ms - 5000ms)
3. **Thả chuột** - Hệ thống tự động cập nhật
4. **Xem thông báo** - Toast notification hiện lên xác nhận

### Giá trị gợi ý:

| Refresh Rate | Mô tả | Khi nào dùng |
|-------------|-------|--------------|
| **100ms** | Rất nhanh | Log có tần suất rất cao |
| **500ms** | Nhanh | Cân bằng tốt |
| **1000ms** | Mặc định | Hầu hết trường hợp |
| **2000ms** | Chậm | Tiết kiệm tài nguyên |
| **5000ms** | Rất chậm | Log ít thay đổi |

### Lưu ý:
- ⚡ Refresh rate càng thấp → CPU làm việc nhiều hơn
- 🐢 Refresh rate càng cao → Độ trễ hiển thị log lớn hơn
- 🔄 Khi thay đổi, tất cả tailer sẽ restart tự động
- ⏱️ Quá trình restart rất nhanh (< 1 giây)

---

## 📖 Hướng dẫn chi tiết

### Chức năng chính:

#### 1. Chọn file log
- Danh sách file log hiển thị ở sidebar bên trái
- Click vào tên file để xem nội dung
- File ở thư mục con hiển thị đầy đủ đường dẫn (vd: `level1/level2/database.log`)

#### 2. Tìm kiếm file
- Sử dụng ô search ở sidebar
- Gõ tên file để lọc nhanh

#### 3. Auto-scroll
- Checkbox "Auto-scroll" ở góc trên
- Bật: Tự động scroll xuống khi có log mới
- Tắt: Giữ nguyên vị trí scroll

#### 4. Clear Console
- Button "Clear Console" xóa tất cả log hiện tại
- Không ảnh hưởng đến file log gốc

#### 5. Color Highlighting
- 🔴 **ERROR**: Màu đỏ đậm
- 🟡 **WARN**: Màu vàng
- 🟢 **INFO**: Màu xanh lá
- 🔵 **DEBUG**: Màu xanh dương

---

## 🔧 API Endpoints

### REST API

**GET /logs**
- Giao diện log monitor

**GET /api/tailer-status**
- Kiểm tra trạng thái các tailer đang hoạt động
- Response:
```json
{
  "count": 3,
  "files": {
    "app.log": "Running",
    "level1/service.log": "Running",
    "level1/level2/database.log": "Running"
  }
}
```

### WebSocket

**WS /log-websocket**
- Endpoint kết nối WebSocket

**WS /topic/logs/{logId}**
- Subscribe để nhận log từ file cụ thể

**WS /topic/new-file**
- Nhận thông báo khi có file log mới

**WS /app/update-refresh-rate**
- Gửi yêu cầu cập nhật refresh rate
- Payload: `{"rate": 500}`

**WS /topic/refresh-rate-updated**
- Nhận thông báo refresh rate đã cập nhật

**WS /topic/refresh-rate-error**
- Nhận thông báo lỗi khi cập nhật refresh rate

---

## 🏗️ Kiến trúc hệ thống

```
┌─────────────────────────────────────────┐
│  Frontend (HTML/JS/Bootstrap)           │
│  - Slider điều chỉnh refresh rate       │
│  - WebSocket client                      │
│  - Toast notifications                   │
└──────────────┬──────────────────────────┘
               │ WebSocket
               ↓
┌─────────────────────────────────────────┐
│  Spring Boot Backend                    │
│  ├─ WebSocketConfig                     │
│  ├─ LogWebSocketController              │
│  │  └─ @MessageMapping update-refresh   │
│  ├─ DynamicLogManagerService            │
│  │  ├─ updateRefreshRate()              │
│  │  ├─ restartAllTailers()              │
│  │  └─ startTailing()                   │
│  └─ LogTailerListener                   │
└──────────────┬──────────────────────────┘
               │ Apache Commons IO Tailer
               ↓
┌─────────────────────────────────────────┐
│  Log Files (D:/logs/**)                 │
│  ├─ app.log                             │
│  ├─ level1/                             │
│  │  ├─ service.log                      │
│  │  └─ level2/                          │
│  │     └─ database.log                  │
│  └─ ...                                 │
└─────────────────────────────────────────┘
```

---

## 🧪 Test và Demo

### Test cơ bản:
1. Khởi động app: `.\mvnw.cmd spring-boot:run`
2. Truy cập: http://localhost:8080/logs
3. Chọn một file log
4. Kéo slider refresh rate về 500ms
5. Quan sát thông báo và log backend

### Test nâng cao:
- Xem file: **TEST_REFRESH_RATE.html**
- Hướng dẫn chi tiết: **REFRESH_RATE_GUIDE.md**

---

## 📝 Changelog

### Version 2.0 - Refresh Rate Control
- ✨ Thêm slider điều chỉnh refresh rate (100ms - 5000ms)
- ✨ WebSocket endpoint để cập nhật refresh rate
- ✨ Toast notification khi cập nhật thành công/thất bại
- ✨ Tự động restart tất cả tailer với refresh rate mới
- 🐛 Fix Thymeleaf security issue với inline event handler
- 📚 Thêm tài liệu hướng dẫn chi tiết

### Version 1.0 - Initial Release
- ✨ Quét đệ quy tất cả thư mục con
- ✨ Tự động phát hiện file log mới
- ✨ WebSocket streaming realtime
- ✨ UI với color highlighting
- ✨ Auto-scroll và search

---

## ❓ FAQ

**Q: Refresh rate thấp có ảnh hưởng gì?**
A: Refresh rate thấp (100ms) làm CPU làm việc nhiều hơn nhưng log hiển thị nhanh hơn. Chỉ dùng khi thực sự cần.

**Q: Có thể set refresh rate khác nhau cho mỗi file không?**
A: Hiện tại chưa hỗ trợ. Refresh rate áp dụng chung cho tất cả file log.

**Q: Khi thay đổi refresh rate, có mất dữ liệu không?**
A: Không. Quá trình restart rất nhanh và không làm mất log.

**Q: Có giới hạn số file log không?**
A: Không có giới hạn cứng, nhưng nên giữ dưới 50 file để đảm bảo hiệu năng.

**Q: Hỗ trợ Windows và Linux?**
A: Có, hỗ trợ cả hai. Chỉ cần đổi đường dẫn LOG_DIRECTORY phù hợp.

---

## 🤝 Support

Nếu gặp vấn đề:
1. Kiểm tra console backend có lỗi không
2. Kiểm tra browser console có lỗi WebSocket không
3. Thử refresh trang và kết nối lại
4. Kiểm tra file TEST_REFRESH_RATE.html để debug

---

## 📚 Tài liệu tham khảo

- **HELP.md**: Maven và Spring Boot references
- **REFRESH_RATE_GUIDE.md**: Chi tiết về refresh rate feature
- **TEST_REFRESH_RATE.html**: Hướng dẫn test đầy đủ

---

**Happy Logging! 🎉**
