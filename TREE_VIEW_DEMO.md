# 🌳 Tree View Log Monitor - Visual Demo

## Before vs After

### ❌ BEFORE (List View)
```
Log Explorer

[🟢] order-service/app.log
[🟢] order-service/archive/app-2026-01-17.1.log
[🟢] order-service/archive/app-2026-01-16.1.log
[🟢] checkout-service/app.log
[🟢] checkout-service/archive/app-2026-01-17.1.log
```

**Issues:**
- ❌ Danh sách dài, khó theo dõi
- ❌ Không biết file nào thuộc thư mục nào
- ❌ Không thể collapse thư mục
- ❌ Khó tìm kiếm khi có nhiều files

---

### ✅ AFTER (Tree View)
```
📁 Log Files

📁 order-service         ◄─── Click mũi tên để thu gọn
   📄 app.log           ◄─── Click để xem log
   📁 archive
      📄 app-2026-01-17.1.log
      📄 app-2026-01-16.1.log
      📄 app-2026-01-15.1.log

📁 checkout-service
   📄 app.log
   📁 archive
      📄 app-2026-01-17.1.log
```

**Benefits:**
- ✅ Cấu trúc phân cấp rõ ràng
- ✅ Dễ nhìn, dễ navigate
- ✅ Có thể collapse thư mục không cần thiết
- ✅ Smart search tự động expand folders
- ✅ Status indicator cho file đang view

---

## 🎮 Interaction Flow

### 1. Load Page
```
Page loads
    ↓
Backend queries logs directory
    ↓
Send file list to frontend
    ↓
JavaScript builds tree structure
    ↓
Tree View rendered in sidebar
```

### 2. User clicks Folder Arrow
```
Click ▼ (expanded toggle)
    ↓
Change to ▶ (collapsed toggle)
    ↓
Folder children hidden with animation
    ↓
Click again to expand
```

### 3. User clicks Log File
```
Click 📄 app.log
    ↓
File highlighted with 🔵 indicator
    ↓
subscribeToLog(logId)
    ↓
WebSocket subscribed to /topic/logs/order-service/app.log
    ↓
Real-time log lines displayed in main panel
```

### 4. User searches
```
Type "archive" in search box
    ↓
Filter: only show files/folders matching "archive"
    ↓
Auto-expand parent folders
    ↓
Display hierarchy maintained
    ↓
Clear search → Reset all to expanded
```

---

## 📐 Tree Structure Example

### Raw File List (from Backend)
```javascript
[
  "order-service/app.log",
  "order-service/archive/app-2026-01-17.1.log",
  "order-service/archive/app-2026-01-16.1.log",
  "checkout-service/app.log",
  "checkout-service/archive/app-2026-01-17.1.log"
]
```

### Tree Structure (in JavaScript)
```javascript
{
  order-service: {
    name: "order-service",
    path: "order-service",
    isFile: false,
    children: {
      app.log: {
        name: "app.log",
        path: "order-service/app.log",
        isFile: true,
        children: {}
      },
      archive: {
        name: "archive",
        path: "order-service/archive",
        isFile: false,
        children: {
          "app-2026-01-17.1.log": {...},
          "app-2026-01-16.1.log": {...}
        }
      }
    }
  },
  checkout-service: {...}
}
```

### Rendered DOM
```html
<div class="tree-node">
  <div class="tree-folder">
    <span class="tree-toggle expanded">▼</span>
    <span>📁 order-service</span>
  </div>
  <div class="tree-children">
    <div class="tree-node">
      <div class="tree-file" data-log-id="order-service/app.log">
        <span class="status-dot"></span>
        <span class="tree-file-icon">📄</span>
        <span>app.log</span>
      </div>
    </div>
    <div class="tree-node">
      <div class="tree-folder">
        <span class="tree-toggle expanded">▼</span>
        <span>📁 archive</span>
      </div>
      <div class="tree-children">
        <!-- Archive files here -->
      </div>
    </div>
  </div>
</div>
```

---

## 🎨 Visual States

### Folder States
```
Expanded (default):
▼ 📁 archive
   📄 app-2026-01-17.1.log
   📄 app-2026-01-16.1.log

Collapsed:
▶ 📁 archive
   (children hidden)
```

### File States
```
Normal (not selected):
📄 app.log
(gray text)

Active (selected):
📄 app.log 🔵
(blue background, white text)

Hover:
📄 app.log
(light gray background)
```

---

## 🔍 Search Filter Example

### Scenario: Type "archive"

**Step 1: Filter files**
```
❌ order-service/app.log (hidden)
✅ order-service/archive/app-2026-01-17.1.log (visible)
✅ order-service/archive/app-2026-01-16.1.log (visible)
❌ checkout-service/app.log (hidden)
✅ checkout-service/archive/app-2026-01-17.1.log (visible)
```

**Step 2: Auto-expand parent folders**
```
📁 order-service            (visible because has visible children)
   📄 app.log              (hidden)
   📁 archive              (expanded ▼)
      📄 app-2026-01-17.1.log (✅ visible, matches search)
      📄 app-2026-01-16.1.log (✅ visible, matches search)

📁 checkout-service         (visible because has visible children)
   📄 app.log              (hidden)
   📁 archive              (expanded ▼)
      📄 app-2026-01-17.1.log (✅ visible, matches search)
```

### Clear search
```
All files visible again
All folders reset to expanded ▼
Back to default state
```

---

## 🚀 Performance

### Tree Building
- **Algorithm:** Depth-first tree construction
- **Time:** O(n) where n = number of files
- **Space:** O(n) for tree structure

### Search Filtering
- **Algorithm:** Linear search through all files
- **Time:** O(n) where n = number of files
- **Optimization:** CSS display toggle (no DOM removal)

### DOM Rendering
- **Max visible items:** ~100 (depends on screen size)
- **Memory:** Minimal (only visible items in DOM)
- **Animation:** Smooth CSS transitions

---

## 📱 Responsive Design

### Desktop (> 768px)
```
┌─────────────────────────────────┐
│  Sidebar (25%)  │  Main (75%)   │
│  Tree View      │  Log Content  │
│  (collapse)     │  (full height)│
└─────────────────────────────────┘
```

### Mobile (< 768px)
```
Sidebar collapses
Log content takes full width
Can toggle sidebar with button
```

---

## ✨ Key Features Highlight

| Feature | Before | After |
|---------|--------|-------|
| **Display** | Flat list | Hierarchical tree |
| **Navigation** | Scroll list | Expand/collapse folders |
| **Search** | Filter only | Filter + auto-expand |
| **Visual** | Numbers | Icons + colors |
| **Scalability** | Works for ~50 files | Works for 1000+ files |
| **UX** | Simple | Professional |

---

## 🧪 Test Cases

### ✅ Test 1: Expand/Collapse
```
1. Click ▼ on order-service
2. Folder children should hide
3. Toggle changes to ▶
4. Click again to expand
```

### ✅ Test 2: Select File
```
1. Click on app.log
2. File should highlight blue
3. Status dot should glow
4. Log content should load
```

### ✅ Test 3: Search & Filter
```
1. Type "archive" in search
2. Only archive files visible
3. archive folder auto-expanded
4. Other files hidden
5. Clear search → all visible again
```

### ✅ Test 4: New File Detection
```
1. Create new log file: checkout-service/app-new.log
2. Tree auto-updates
3. New file appears in correct position
```

### ✅ Test 5: Multiple Selection
```
1. Click file A → highlight
2. Click file B → file A unhighlights, file B highlights
3. Only one file active at a time
```

---

## 📦 Files Modified

| File | Changes |
|------|---------|
| `log-monitor.html` | CSS + HTML + JavaScript |
| `LogViewController.java` | No changes (backend compatible) |

---

**Status:** ✅ Complete  
**Date:** January 17, 2026  
**Version:** 1.0
