
## ⚙️ Setup Backend (chạy local)
### Yêu cầu
- Java 21+
- MySQL 9
- Maven 3.8+

### Bước 1 — Clone và cấu hình
```bash
git clone <repo-url>
cd backend
```

Mở `src/main/resources/application.properties`, chỉnh:
```properties
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD
app.jwt.secret=YOUR_SECRET_KEY_MIN_32_CHARS
```

### Bước 2 — Chạy server
```bash
mvn spring-boot:run
```
Server khởi động tại `http://localhost:8080`.  
Flyway tự động tạo database và migrate schema.  
Data mẫu tự động import từ `csvdata/` khi start lần đầu.

### Bước 3 — Kiểm tra
Gọi thử:
```
GET http://localhost:8080/api/v1/user
```
Nếu trả về `401` → server đang chạy đúng (cần token).

---


## 🌐 Môi trường

| Môi trường | Base URL |
|---|---|
| Local | `http://localhost:8080` |
| Android Emulator | `http://10.0.2.2:8080` |
| iOS Simulator | `http://localhost:8080` |

**Token:**
- `accessToken` hết hạn sau **24 giờ**
- `refreshToken` hết hạn sau **7 ngày**
- Khi `accessToken` hết hạn → gọi `/api/v1/auth/refresh` để lấy token mới

---
s
## 🔄 Flow tổng thể

```
1. Đăng ký / Đăng nhập
   POST /api/v1/auth/register  hoặc  POST /api/v1/auth/login
   → nhận được { accessToken, refreshToken, userId, email, ... }

2. Lưu token xuống local storage / shared preferences

3. Gọi các API khác
   Header: Authorization: Bearer <accessToken>

4. Kết nối WebSocket (realtime)
   ws://localhost:8080/ws
   Header CONNECT: Authorization: Bearer <accessToken>

5. Khi accessToken hết hạn (API trả về 401)
   POST /api/v1/auth/refresh  với { refreshToken }
   → nhận accessToken mới → gọi lại API

6. Đăng xuất
   POST /api/v1/auth/logout
   → xóa token khỏi local storage
   → ngắt kết nối WebSocket
```

---

## 🔐 Xác thực (Auth)

### Đăng ký
```
POST /api/v1/auth/register
```
**Request body:**
```json
{
  "email": "user@gmail.com",
  "password": "123456",
  "firstName": "Nguyen",
  "lastName": "Van A"
}
```
**Response `201`:**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "user@gmail.com",
  "firstName": "Nguyen",
  "lastName": "Van A",
  "online": false
}
```

---

### Đăng nhập
```
POST /api/v1/auth/login
```
**Request body:**
```json
{
  "email": "user@gmail.com",
  "password": "123456"
}
```
**Response `200`:** _(giống register)_

---

### Làm mới token
```
POST /api/v1/auth/refresh
```
**Request body:**
```json
{
  "refreshToken": "eyJhbGci..."
}
```
**Response `200`:** _(trả về accessToken mới, refreshToken cũ giữ nguyên)_

---

### Đăng xuất
```
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>
```
**Response `200`:** _(không có body)_

---

## 👤 Người dùng (User)

### Lấy danh sách tất cả user (trừ bản thân)
```
GET /api/v1/user
Authorization: Bearer <accessToken>
```
**Response `200`:**
```json
[
  {
    "id": "uuid",
    "firstName": "Nguyen",
    "lastName": "Van A",
    "email": "user@gmail.com",
    "online": true,
    "lastSeenText": "Đang hoạt động"
  }
]
```

---

### Lấy thông tin bản thân
```
GET /api/v1/user/me
Authorization: Bearer <accessToken>
```

---

### Tìm kiếm user
```
GET /api/v1/user/search?keyword=nguyen
Authorization: Bearer <accessToken>
```

---

## 💬 Chat 1-1

### Lấy danh sách chat
```
GET /api/v1/chat
Authorization: Bearer <accessToken>
```
**Response `200`:**
```json
[
  {
    "id": "uuid",
    "user1Id": "uuid",
    "user2Id": "uuid",
    "chatName": "Nguyen Van A",
    "lastMessage": "Xin chào!",
    "lastMessageType": "TEXT",
    "lastMessageTime": "2025-03-15T10:00:00",
    "unreadCount": 2,
    "recipientOnline": true,
    "recipientLastSeenText": "Đang hoạt động"
  }
]
```

---

### Bắt đầu chat với user
```
POST /api/v1/chat/start/{otherUserId}
Authorization: Bearer <accessToken>
```
_Tạo mới nếu chưa có, trả về chat hiện có nếu đã tồn tại._

---

### Lấy chi tiết chat
```
GET /api/v1/chat/{chatId}
Authorization: Bearer <accessToken>
```

---

### Lấy tin nhắn trong chat
```
GET /api/v1/chat/{chatId}/messages
Authorization: Bearer <accessToken>
```
**Response `200`:**
```json
[
  {
    "id": "uuid",
    "chatId": "uuid",
    "content": "Xin chào!",
    "state": "SENT",
    "type": "TEXT",
    "createdAt": "2025-03-15T10:00:00",
    "senderId": "uuid",
    "receiverId": "uuid",
    "mediaUrl": null
  }
]
```

---

### Đánh dấu đã đọc
```
POST /api/v1/chat/{chatId}/read
Authorization: Bearer <accessToken>
```
**Response `200`:** _(không có body)_

---

## 📨 Tin nhắn (Message)

### Gửi tin nhắn text
```
POST /api/v1/message
Authorization: Bearer <accessToken>
Content-Type: application/json
```
**Request body:**
```json
{
  "chatId": "uuid",
  "content": "Xin chào!",
  "type": "TEXT",
  "senderId": "uuid",
  "receiverId": "uuid"
}
```
**Response `202`:** _(không có body, tin nhắn được đẩy qua WebSocket)_

---

### Upload media (ảnh/video/file)
```
POST /api/v1/message/upload-media/{chatId}
Authorization: Bearer <accessToken>
Content-Type: multipart/form-data
```
**Form field:** `file` — file cần upload  
**Response `202`:** _(không có body, tin nhắn media được đẩy qua WebSocket)_

---

### Lấy file media
```
GET /api/v1/message/media/{filename}
```
_Endpoint **public**, không cần token._

---

### Đánh dấu đã xem
```
PATCH /api/v1/message/seen/{chatId}
Authorization: Bearer <accessToken>
```
**Response `200`:** _(không có body)_

---

### Lấy tin nhắn theo chatId (phân trang)
```
GET /api/v1/message/chat/{chatId}?page=0&size=30
Authorization: Bearer <accessToken>
```
**Response `200`:**
```json
[
  {
    "id": "uuid",
    "chatId": "uuid",
    "content": "Xin chào!",
    "state": "SEEN",
    "type": "TEXT",
    "createdAt": "2025-03-15T10:00:00",
    "senderId": "uuid",
    "receiverId": "uuid",
    "mediaUrl": null
  }
]
```

---

## 👥 Nhóm chat (Group)

### Tạo nhóm mới
```
POST /api/v1/group
Authorization: Bearer <accessToken>
```
**Request body:**
```json
{
  "name": "Nhóm học tập",
  "description": "Nhóm ôn thi cuối kỳ",
  "memberIds": ["uuid1", "uuid2", "uuid3"]
}
```
**Response `201`:**
```json
{
  "id": "uuid",
  "name": "Nhóm học tập",
  "description": "Nhóm ôn thi cuối kỳ",
  "avatarUrl": null,
  "createdById": "uuid",
  "memberCount": 4,
  "members": [
    {
      "userId": "uuid",
      "firstName": "Nguyen",
      "lastName": "Van A",
      "email": "user@gmail.com",
      "admin": true,
      "online": true,
      "lastSeenText": "Đang hoạt động"
    }
  ],
  "lastMessage": null,
  "isAdmin": true
}
```

---

### Lấy danh sách nhóm của tôi
```
GET /api/v1/group
Authorization: Bearer <accessToken>
```

---

### Lấy chi tiết nhóm
```
GET /api/v1/group/{groupId}
Authorization: Bearer <accessToken>
```

---

### Cập nhật thông tin nhóm _(chỉ admin)_
```
PUT /api/v1/group/{groupId}
Authorization: Bearer <accessToken>
```
**Request body:**
```json
{
  "name": "Tên mới",
  "description": "Mô tả mới",
  "avatarUrl": "https://..."
}
```

---

### Thêm thành viên _(chỉ admin)_
```
POST /api/v1/group/{groupId}/members
Authorization: Bearer <accessToken>
```
**Request body:**
```json
{
  "userIds": ["uuid1", "uuid2"]
}
```

---

### Xóa thành viên _(chỉ admin)_
```
DELETE /api/v1/group/{groupId}/members/{userId}
Authorization: Bearer <accessToken>
```

---

### Rời nhóm
```
DELETE /api/v1/group/{groupId}/leave
Authorization: Bearer <accessToken>
```

---

### Gửi tin nhắn vào nhóm
```
POST /api/v1/group/{groupId}/messages
Authorization: Bearer <accessToken>
```
**Request body:**
```json
{
  "content": "Xin chào nhóm!",
  "type": "TEXT"
}
```
**Response `201`:**
```json
{
  "id": "uuid",
  "content": "Xin chào nhóm!",
  "type": "TEXT",
  "groupId": "uuid",
  "senderId": "uuid",
  "senderName": "Nguyen Van A",
  "isMine": true,
  "createdDate": "2025-03-15T10:00:00"
}
```

---

### Lấy tin nhắn nhóm (phân trang)
```
GET /api/v1/group/{groupId}/messages?page=0&size=30
Authorization: Bearer <accessToken>
```

---

## 🔌 WebSocket (Realtime)

### Kết nối
**URL:** `ws://localhost:8080/ws`  
**Protocol:** STOMP over SockJS  
**Header khi CONNECT:** `Authorization: Bearer <accessToken>`

### Các topic cần subscribe

| Topic | Mô tả |
|---|---|
| `/user/{email}/queue/messages` | Nhận tin nhắn chat 1-1 mới |
| `/topic/group/{groupId}` | Nhận tin nhắn nhóm mới |
| `/topic/user-status` | Nhận trạng thái online/offline |

---

## 📊 Enums

### MessageType
| Giá trị | Mô tả |
|---|---|
| `TEXT` | Tin nhắn văn bản |
| `IMAGE` | Hình ảnh |
| `VIDEO` | Video |
| `DOCUMENT` | Tài liệu |
| `AUDIO` | Âm thanh |

### MessageState
| Giá trị | Mô tả |
|---|---|
| `SENT` | Đã gửi |
| `DELIVERED` | Đã nhận |
| `SEEN` | Đã xem |

---

## ⚠️ Mã lỗi

| HTTP Code | Ý nghĩa |
|---|---|
| `400` | Dữ liệu đầu vào không hợp lệ |
| `401` | Chưa đăng nhập hoặc token hết hạn |
| `403` | Không có quyền thực hiện |
| `404` | Không tìm thấy tài nguyên |
| `409` | Xung đột dữ liệu (VD: email đã tồn tại) |
| `500` | Lỗi hệ thống |

**Response lỗi thông thường:**
```json
{
  "timestamp": "2025-03-15T10:00:00",
  "status": 401,
  "error": "Email hoặc mật khẩu không đúng"
}
```

**Response lỗi validation (400):**
```json
{
  "timestamp": "2025-03-15T10:00:00",
  "status": 400,
  "error": "Validation failed",
  "details": {
    "email": "Email không hợp lệ",
    "password": "Mật khẩu tối thiểu 6 ký tự"
  }
}
```

---

## 🗄️ Database Schema

| Bảng | Mô tả |
|---|---|
| `user` | Tài khoản người dùng |
| `chat` | Cuộc hội thoại 1-1 |
| `message` | Tin nhắn chat 1-1 |
| `group` | Nhóm chat |
| `group_member` | Thành viên nhóm |
| `group_message` | Tin nhắn nhóm |
| `flyway_schema_history` | Lịch sử migration DB |

---

## 👥 Tài khoản mẫu (test)

> Password mặc định tất cả: `123456`

| Email | Tên | Ghi chú |
|---|---|---|
| an.nguyen@gmail.com | An Nguyen | Online |
| binh.tran@gmail.com | Binh Tran | Online |
| chau.le@gmail.com | Chau Le | Online |
| dung.pham@gmail.com | Dung Pham | - |
| em.hoang@gmail.com | Em Hoang | - |
| phuc.vo@gmail.com | Phuc Vo | - |
| giang.dang@gmail.com | Giang Dang | - |
| hoa.bui@gmail.com | Hoa Bui | - |
| kiet.do@gmail.com | Kiet Do | - |
| lan.ngo@gmail.com | Lan Ngo | - |

---

*Phiên bản: v3 — Cập nhật: 03/2026*