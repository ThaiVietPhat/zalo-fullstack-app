
## 🚀 Khởi động

### Yêu cầu
- Java 21+
- MySQL 9
- Maven 3.8+

### Cấu hình
Mở file `src/main/resources/application.properties`, chỉnh các biến:

```properties
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD
app.jwt.secret=YOUR_SECRET_KEY_MIN_32_CHARS
```

### Chạy
```bash
mvn spring-boot:run
```
Server khởi động tại: `http://localhost:8080`  
Flyway tự động tạo/migrate database khi start.

---

## 🔐 Xác thực (Auth)

Tất cả API (trừ register/login) yêu cầu header:
```
Authorization: Bearer <accessToken>
```

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
  "userId": "uuid",
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
**Response `200`:** _(trả về accessToken mới)_

---

### Đăng xuất
```
POST /api/v1/auth/logout
```
_Header: Authorization required_  
**Response `200`:** _(không có body)_

---

## 👤 Người dùng (User)

### Lấy danh sách tất cả user (trừ bản thân)
```
GET /api/v1/user
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
```

---

### Tìm kiếm user
```
GET /api/v1/user/search?keyword=nguyen
```

---

## 💬 Chat 1-1 (Chat)

### Lấy danh sách chat
```
GET /api/v1/chat
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

### Lấy chi tiết chat
```
GET /api/v1/chat/{chatId}
```

---

### Bắt đầu chat với user
```
POST /api/v1/chat/start/{otherUserId}
```
_Tạo mới nếu chưa có, trả về chat hiện có nếu đã tồn tại._

---

### Lấy tin nhắn trong chat
```
GET /api/v1/chat/{chatId}/messages
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
```

---

## 📨 Tin nhắn (Message)

### Gửi tin nhắn text
```
POST /api/v1/message
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

---

### Upload media (ảnh/video/file)
```
POST /api/v1/message/upload-media/{chatId}
Content-Type: multipart/form-data
```
**Form field:** `file` — file cần upload  
**Response `202`:** _(không có body, tin nhắn được gửi qua WebSocket)_

---

### Lấy file media
```
GET /api/v1/message/media/{filename}
```
_Endpoint public, không cần token._

---

### Đánh dấu tin nhắn đã xem
```
PATCH /api/v1/message/seen/{chatId}
```

---

### Lấy tin nhắn theo chatId (phân trang)
```
GET /api/v1/message/chat/{chatId}?page=0&size=30
```

---

## 👥 Nhóm chat (Group)

### Tạo nhóm mới
```
POST /api/v1/group
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
  "lastMessageType": null,
  "lastMessageTime": null,
  "lastMessageSenderName": null,
  "isAdmin": true
}
```

---

### Lấy danh sách nhóm của tôi
```
GET /api/v1/group
```

---

### Lấy chi tiết nhóm
```
GET /api/v1/group/{groupId}
```

---

### Cập nhật thông tin nhóm _(chỉ admin)_
```
PUT /api/v1/group/{groupId}
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
```

---

### Rời nhóm
```
DELETE /api/v1/group/{groupId}/leave
```
_Nếu admin rời nhóm, quyền admin tự động chuyển cho thành viên tiếp theo.  
Nếu không còn ai, nhóm bị xóa._

---

### Gửi tin nhắn vào nhóm
```
POST /api/v1/group/{groupId}/messages
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
```

---

## 🔌 WebSocket (Realtime)

### Kết nối
```
ws://localhost:8080/ws
```
**Header khi CONNECT:**
```
Authorization: Bearer <accessToken>
```

### Subscribe nhận tin nhắn chat 1-1
```
/user/{email}/queue/messages
```

### Subscribe nhận tin nhắn nhóm
```
/topic/group/{groupId}
```

### Subscribe trạng thái online/offline
```
/topic/user-status
```

### Gửi tin nhắn qua WebSocket
```
/app/chat
```

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

**Response lỗi mẫu:**
```json
{
  "timestamp": "2025-03-15T10:00:00",
  "status": 401,
  "error": "Email hoặc mật khẩu không đúng"
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
| `flyway_schema_history` | Lịch sử migration |

---

*Phiên bản: v3 — Cập nhật: 03/2026*