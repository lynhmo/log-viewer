# 📋 Tree View Log Files - Hướng dẫn cập nhật

## ✅ Đã hoàn thành

Sidebar hiển thị log files đã được chỉnh sửa để hiển thị dạng **cây phân cấp (Tree View)** thay vì danh sách thông thường.

## 🎨 Các tính năng

### 1. **Cấu trúc cây (Tree Structure)**
```
📁 order-service
   ├── 📄 app.log
   └── 📁 archive
       ├── 📄 app-2026-01-17.1.log
       ├── 📄 app-2026-01-16.1.log
       └── 📄 app-2026-01-15.1.log
📁 checkout-service
   ├── 📄 app.log
   └── 📁 archive
       ├── 📄 app-2026-01-17.1.log
       └── ...
```

### 2. **Mở/Thu gọn thư mục**
- Click vào mũi tên (▶/▼) để mở/thu gọn thư mục
- Double-click tên thư mục cũng mở/thu gọn

### 3. **Chọn file log**
- Click vào file (📄) để xem log
- File được chọn sẽ highlight xanh
- Status dot (chấm xanh) sẽ sáng lên khi active

### 4. **Tìm kiếm nhanh**
- Nhập tên file/thư mục vào search box
- Tự động mở các thư mục cha nếu có kết quả match
- Clear search để reset về trạng thái mặc định

### 5. **Tự động cập nhật**
- Khi có file log mới được tạo, tree view tự động thêm vào
- Không cần F5 reload trang

## 🔧 Changes Made

### HTML/CSS
```html
<!-- Trước (List View) -->
<div class="list-group" id="file-list">
    <button class="list-group-item ...">
        order-service/app.log
    </button>
</div>

<!-- Sau (Tree View) -->
<div class="tree-view" id="file-list">
    <!-- Tạo từ JavaScript -->
</div>
```

### CSS Classes
```css
.tree-view          /* Container chứa tree */
.tree-node          /* Node element */
.tree-folder        /* Folder node */
.tree-file          /* File node */
.tree-toggle        /* Mũi tên expand/collapse */
.tree-children      /* Children container */
.status-dot         /* Indicator dot */
```

### JavaScript Functions
```javascript
buildFileTree(files)              // Build toàn bộ tree
buildTreeStructure(files)         // Tạo structure object
createTreeNode(node, level)       // Tạo DOM element
selectFile(logId, element)        // Chọn file
attachTreeListeners()             // Attach event listeners
```

## 💡 Cách thức hoạt động

1. **Backend** (`LogViewController.java`)
   - Quét thư mục logs
   - Tìm tất cả file `.log`
   - Gửi list files dạng string (VD: `order-service/app.log`)

2. **Frontend** (Thymeleaf)
   - Truyền files via `data-log-id-initial` attributes

3. **JavaScript**
   - Đọc files từ data attributes
   - Xây dựng tree structure
   - Render DOM elements
   - Attach event listeners

4. **User Interaction**
   - Click file → `selectFile()`
   - Click mũi tên → Toggle folder
   - Type search → Filter + auto-expand

## 🎯 Ví dụ

### Thư mục logs thực tế
```
logs/
├── order-service/
│   ├── app.log
│   └── archive/
│       ├── app-2026-01-17.1.log
│       └── app-2026-01-16.1.log
└── checkout-service/
    ├── app.log
    └── archive/
        └── app-2026-01-17.1.log
```

### Tree View hiển thị
```
📁 order-service
   📄 app.log
   📁 archive
      📄 app-2026-01-17.1.log
      📄 app-2026-01-16.1.log
📁 checkout-service
   📄 app.log
   📁 archive
      📄 app-2026-01-17.1.log
```

## 🔍 Search Example

### Tìm kiếm "archive"
```
📁 order-service           (ẩn)
   📄 app.log             (ẩn)
   📁 archive             (hiển thị + auto expand)
      📄 app-2026-01-17.1.log  (hiển thị)
      📄 app-2026-01-16.1.log  (hiển thị)
```

### Clear search
Trở lại trạng thái mặc định (tất cả expand)

## 📱 Responsive

Tree view:
- ✅ Chiều cao tự động scroll
- ✅ Responsive font size
- ✅ Hoạt động tốt trên mobile

## 🐛 Testing

### Test collapse/expand
1. Click vào mũi tên thư mục
2. Kiểm tra thư mục con ẩn/hiện

### Test file selection
1. Click vào file `.log`
2. Kiểm tra highlight xanh
3. Kiểm tra log content thay đổi

### Test search
1. Nhập "archive"
2. Kiểm tra chỉ hiển thị archive files
3. Kiểm tra thư mục cha auto expand
4. Clear search → trở lại normal

### Test new file
1. Tạo file log mới trong thư mục
2. Kiểm tra tree auto update

## 📝 Files Changed

1. ✅ `log-monitor.html`
   - CSS: Thêm `.tree-*` classes
   - HTML: Thay `.list-group` → `.tree-view`
   - JS: Thêm tree functions
   - JS: Update search filter

## 🚀 Usage

Tree view sẽ tự động hiển thị khi trang load. Không cần setup thêm!

---

**Status:** ✅ Ready  
**Date:** January 17, 2026
