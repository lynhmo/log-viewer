# 📅 Historical Log Viewer - Xem Log Cũ

## ✅ Tính năng mới

Người dùng có thể:
1. ✅ Chọn một file log
2. ✅ Nhập ngày tháng 
3. ✅ Nhập số dòng cần xem (mặc định 100)
4. ✅ Click "Load" để xem log cũ

## 🎮 Cách sử dụng

### Step 1: Chọn File Log
```
1. Click vào file log trong sidebar
   VD: order-service/app.log
```

### Step 2: Chọn Ngày
```
2. Chọn ngày từ Date Picker
   📅 Date: [YYYY-MM-DD]
   
   Lưu ý: Date picker sẽ tự load danh sách ngày có sẵn
         từ thư mục archive
```

### Step 3: Nhập Số Lượng Dòng
```
3. Nhập số lượng dòng log cần xem
   📊 Lines: [100]  (có thể nhập từ 1 - 10000)
```

### Step 4: Load Log Cũ
```
4. Click nút "Load" để lấy log cũ
   
   Kết quả:
   === Historical Logs from 2026-01-17 (100/500 lines) ===
   [ERROR] Exception in application
   [WARN] Connection timeout
   [INFO] Application started
   ...
```

---

## 📂 File Structure Example

### Thư mục logs
```
logs/
├── order-service/
│   ├── app.log                          (current log)
│   └── archive/
│       ├── app-2026-01-17.1.log         (yesterday)
│       ├── app-2026-01-16.1.log
│       ├── app-2026-01-15.1.log
│       ├── app-2026-01-14.1.log
│       └── app-2026-01-13.1.log
└── checkout-service/
    ├── app.log
    └── archive/
        ├── app-2026-01-17.1.log
        └── app-2026-01-16.1.log
```

### UI Controls
```
┌─────────────────────────────────────────────────────────┐
│ Select log: order-service/app.log                       │
├─────────────────────────────────────────────────────────┤
│ 📅 Date: [2026-01-17]  📊 Lines: [100]  [Load]         │
│ Refresh: [═══════] 1000ms  ☑ Auto-scroll  [Clear]      │
├─────────────────────────────────────────────────────────┤
│ === Historical Logs from 2026-01-17 (100/500 lines) ===│
│ [ERROR] Exception occurred                              │
│ [WARN] Retry count exceeded                             │
│ [INFO] Service initialized                              │
│ ...                                                      │
└─────────────────────────────────────────────────────────┘
```

---

## 🔧 Backend API

### 1. `/api/available-dates`
**Lấy danh sách ngày có log cũ**

Request:
```
GET /api/available-dates?logFile=order-service/app.log
```

Response:
```json
{
  "success": true,
  "logFile": "order-service/app.log",
  "dates": [
    "2026-01-13",
    "2026-01-14",
    "2026-01-15",
    "2026-01-16",
    "2026-01-17"
  ]
}
```

### 2. `/api/historical-logs`
**Lấy log cũ theo ngày và số dòng**

Request:
```
GET /api/historical-logs?logFile=order-service/archive/app-2026-01-17.1.log&lineCount=100
```

Response:
```json
{
  "success": true,
  "file": "order-service/archive/app-2026-01-17.1.log",
  "totalLines": 1500,
  "loadedLines": 100,
  "lines": [
    "2026-01-17 10:15:23 [ERROR] Exception: Connection timeout",
    "2026-01-17 10:15:24 [WARN] Retry attempt 1/3",
    "2026-01-17 10:15:25 [INFO] Service restarted",
    ...
  ]
}
```

---

## 🧪 Test Cases

### Test 1: Load Available Dates
```
1. Chọn file log: order-service/app.log
2. Kiểm tra: Date picker có min/max value
3. Kiểm tra: Có ngày được selected mặc định
```

### Test 2: Load Historical Logs
```
1. Chọn file: order-service/app.log
2. Chọn ngày: 2026-01-15
3. Nhập dòng: 50
4. Click Load
5. Kiểm tra: Logs từ 2026-01-15 được hiển thị
6. Kiểm tra: Hiển thị "Historical Logs from 2026-01-15 (50/xxx lines)"
```

### Test 3: Different Line Counts
```
1. Thay đổi Line count thành 500
2. Click Load
3. Kiểm tra: 500 dòng được load (nếu file có)
```

### Test 4: Highlight Logic
```
1. Load historical logs
2. Kiểm tra:
   - [ERROR] lines = Red
   - [WARN] lines = Yellow
   - [INFO] lines = Green
   - [DEBUG] lines = Blue
```

### Test 5: No Archive Files
```
1. Chọn file: checkout-service/app.log
2. Kiểm tra: Date picker empty hoặc disabled
3. Nhập ngày manual
4. Click Load
5. Kiểm tra: Error message "File not found"
```

---

## 💡 Implementation Details

### Frontend (JavaScript)

#### 1. `loadAvailableDates(logId)`
- Called khi user click file
- Fetch `/api/available-dates`
- Set date picker min/max
- Auto-select first available date

```javascript
loadAvailableDates("order-service/app.log")
  → fetch /api/available-dates
  → get dates: ["2026-01-13", "2026-01-14", ...]
  → set dateInput.min = "2026-01-13"
  → set dateInput.max = "2026-01-17"
  → dateInput.value = "2026-01-13"
```

#### 2. `loadHistoricalLogs()`
- Called khi user click "Load"
- Validate: File selected & Date selected
- Build path: "service/archive/app-YYYY-MM-DD.1.log"
- Fetch `/api/historical-logs`
- Clear console
- Render logs with highlighting

```javascript
loadHistoricalLogs()
  → get currentFile = "order-service/app.log"
  → get logDate = "2026-01-17"
  → get lineCount = "100"
  → build path = "order-service/archive/app-2026-01-17.1.log"
  → fetch /api/historical-logs
  → render logs in log-content
  → show notification: "Loaded 100 lines from 2026-01-17"
```

### Backend (Java)

#### 1. `getAvailableDates(logFile)`
- Extract service name: "order-service/app.log" → "order-service"
- Build archive path: "logs/order-service/archive"
- List all .log files in archive
- Extract dates: "app-2026-01-17.1.log" → "2026-01-17"
- Return sorted distinct dates

#### 2. `getHistoricalLogs(logFile, lineCount)`
- Validate file exists
- Read all lines from file
- Calculate start index: `Math.max(0, allLines.size() - lineCount)`
- Return subset of lines + metadata

---

## 🎯 Features

| Feature | Details |
|---------|---------|
| **Date Picker** | Auto-populated with available dates |
| **Line Count** | Customizable (1-10000) |
| **Highlighting** | ERROR/WARN/INFO/DEBUG colors |
| **Metadata** | Shows total lines & loaded lines |
| **Error Handling** | Shows notification on error |
| **Auto-scroll** | Scrolls to top after load |
| **Validation** | Checks file & date selected |

---

## 📊 Example Workflow

```
User Session Flow
│
├─ Load page
│  └─ WebSocket connected
│
├─ Click: order-service/app.log
│  ├─ File selected & highlighted
│  ├─ Subscribe to real-time logs
│  └─ Load available dates (API call)
│      ├─ Get archive dates
│      ├─ Set date picker range
│      └─ Auto-select first date
│
├─ Date: 2026-01-15, Lines: 200
│  └─ Click "Load" button
│      ├─ Fetch /api/historical-logs
│      │  └─ Read logs/order-service/archive/app-2026-01-15.1.log
│      ├─ Display: "Historical Logs from 2026-01-15 (200/800 lines)"
│      └─ Show 200 last lines with highlighting
│
├─ View logs in console
│  ├─ Scroll & read old logs
│  └─ No real-time updates (historical view)
│
└─ Click: order-service/app.log again
   └─ Resume real-time monitoring
      └─ Live logs streaming
```

---

## 🚀 File Changes

### Backend
```
✅ LogMonitorController.java
   ├─ getAvailableDates() - New endpoint
   └─ getHistoricalLogs() - New endpoint

✅ DynamicLogManagerService.java
   └─ getLogDirectory() - New getter method
```

### Frontend
```
✅ log-monitor.html
   ├─ HTML: Add date & line count inputs
   ├─ HTML: Add "Load" button
   ├─ JS: loadAvailableDates()
   ├─ JS: loadHistoricalLogs()
   └─ JS: Updated selectFile()
```

---

## 📝 Notes

1. **Archive Format:** Files must follow `app-YYYY-MM-DD.1.log` naming
2. **Logback Configuration:** logback-spring.xml should use this format
3. **Performance:** Large files (>100MB) may load slowly
4. **Memory:** Loading millions of lines may use significant memory
5. **Date Range:** Only shows dates where archive files exist

---

## 🎓 Code Example

### Load historical logs for a specific date
```javascript
// Fetch API
fetch('/api/historical-logs?logFile=order-service/archive/app-2026-01-17.1.log&lineCount=100')
  .then(response => response.json())
  .then(data => {
    if (data.success) {
      data.lines.forEach(line => {
        const div = document.createElement('div');
        div.textContent = line;
        document.getElementById('log-content').appendChild(div);
      });
    }
  });
```

---

**Status:** ✅ Complete  
**Date:** January 17, 2026
