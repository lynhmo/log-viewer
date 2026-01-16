# Hướng dẫn sử dụng chức năng điều chỉnh Refresh Rate

## Tính năng mới: Điều chỉnh tốc độ làm mới log

Bạn có thể thay đổi tốc độ làm mới (refresh rate) của log monitor theo nhu cầu thực tế.

### Cách sử dụng:

1. **Mở giao diện Log Monitor**
   - Truy cập: `http://localhost:8080/logs`

2. **Tìm thanh Refresh Rate**
   - Ở góc trên bên phải giao diện, bạn sẽ thấy:
     ```
     Refresh Rate: 1000ms [────slider────]
     ```

3. **Điều chỉnh tốc độ**
   - Kéo thanh slider sang trái/phải để thay đổi giá trị
   - Giá trị từ **100ms** đến **5000ms**
   - Giá trị hiện tại sẽ hiển thị bên cạnh slider

4. **Áp dụng thay đổi**
   - Khi bạn thả chuột (sau khi kéo slider), hệ thống sẽ:
     - Tự động cập nhật tốc độ cho tất cả các file log đang tail
     - Hiển thị thông báo xác nhận ở góc trên bên phải
     - Log trên console backend sẽ hiển thị quá trình restart

### Giải thích Refresh Rate:

- **100ms** (0.1 giây): Cập nhật rất nhanh, phù hợp cho log có tần suất cao
- **500ms** (0.5 giây): Cập nhật nhanh, cân bằng giữa hiệu năng và độ trễ
- **1000ms** (1 giây): Mặc định, phù hợp cho hầu hết trường hợp
- **2000ms** (2 giây): Cập nhật chậm hơn, tiết kiệm tài nguyên
- **5000ms** (5 giây): Cập nhật rất chậm, phù hợp cho log ít thay đổi

### Lưu ý:

- Refresh rate càng thấp (100ms), CPU sẽ làm việc nhiều hơn
- Refresh rate càng cao (5000ms), độ trễ hiển thị log càng lớn
- Khi thay đổi refresh rate, tất cả các tailer sẽ được **restart tự động**
- Quá trình restart diễn ra rất nhanh (< 1 giây)

### Thông báo:

- ✅ **Thành công**: Màu xanh - "Refresh rate đã được cập nhật: XXXms"
- ❌ **Lỗi**: Màu đỏ - "Lỗi khi cập nhật refresh rate: [chi tiết lỗi]"

### Backend logs:

Khi bạn thay đổi refresh rate, console backend sẽ hiển thị:
```
Nhận yêu cầu cập nhật refresh rate: 500ms
Đã cập nhật refresh rate: 500ms
Đang restart 3 tailer(s)...
Restart tail file: app.log với refresh rate 500ms
Restart tail file: level1/service.log với refresh rate 500ms
Restart tail file: level1/level2/database.log với refresh rate 500ms
Đã restart xong 3 tailer(s)
```

## Technical Details

### Kiến trúc:

1. **Frontend (HTML + JavaScript)**
   - Slider input với range 100-5000ms
   - Event listener để gửi yêu cầu qua WebSocket
   - Subscribe topic để nhận thông báo cập nhật

2. **WebSocket Controller**
   - `@MessageMapping("/update-refresh-rate")`
   - Nhận request từ client
   - Gọi service để cập nhật

3. **DynamicLogManagerService**
   - `updateRefreshRate(long newRateMs)`: Cập nhật biến refreshRateMs
   - `restartAllTailers()`: Restart tất cả tailer
   - Mỗi tailer mới sử dụng giá trị refreshRateMs hiện tại

### Flow:

```
User kéo slider 
  → Frontend gửi WebSocket message
    → WebSocketController nhận message
      → Service cập nhật refreshRateMs
        → Service restart tất cả tailer
          → Mỗi tailer mới dùng refreshRateMs mới
            → Gửi thông báo thành công về Frontend
              → Hiển thị toast notification
```
push