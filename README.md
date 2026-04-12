# Zalo Clone — Backend API

Backend REST API cho ứng dụng chat Zalo Clone, hỗ trợ **mobile (Android/iOS)** và **web**.

**Tech stack:** Spring Boot 3.4.2 · Java 21 · MySQL 9 · WebSocket (STOMP/SockJS) · JWT · Claude AI

---

## Mục lục

1. [Yêu cầu & cài đặt](#1-yêu-cầu--cài-đặt)
2. [Biến môi trường](#2-biến-môi-trường)
3. [Base URL](#3-base-url)
4. [Xác thực JWT](#4-xác-thực-jwt)
5. [Flow tích hợp](#5-flow-tích-hợp)
6. [REST API](#6-rest-api)
7. [Request / Response chi tiết](#7-request--response-chi-tiết)
8. [WebSocket Realtime](#8-websocket-realtime)
9. [Enums](#9-enums)
10. [Mã lỗi](#10-mã-lỗi)
11. [Database Schema](#11-database-schema)
12. [Tài khoản mẫu](#12-tài-khoản-mẫu)

---

## 1. Yêu cầu & cài đặt

### Yêu cầu
- Java 21+
- MySQL 9
- Maven 3.8+ (hoặc dùng `./mvnw` đã có sẵn)

### Bước 1 — Clone

```bash
git clone <repo-url>
cd zalo-clone/backend
```

### Bước 2 — Tạo database MySQL

```sql
CREATE DATABASE zalo_clone CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Bước 3 — Cấu hình

Mở `src/main/resources/application.properties` và chỉnh các dòng sau:

```properties
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD
app.jwt.secret=YOUR_SECRET_KEY_MIN_32_CHARS_HERE
app.claude.api-key=YOUR_CLAUDE_API_KEY          # tùy chọn — bỏ trống nếu không dùng AI
```

Hoặc truyền qua biến môi trường (xem [mục 2](#2-biến-môi-trường)).

### Bước 4 — Chạy

```bash
./mvnw spring-boot:run
```

Hoặc build JAR rồi chạy:

```bash
./mvnw clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

**Lần đầu chạy:**
- Flyway tự động tạo toàn bộ schema (bảng, index, foreign key).
- `DataSeeder` tạo tài khoản admin mặc định (`admin@zalo.com / Admin@123`) và import dữ liệu mẫu từ thư mục `src/main/resources/csvdata/` (chat, message, group...).

### Kiểm tra nhanh

```bash
curl http://localhost:8080/api/v1/user
# → 401 Unauthorized  ✅  (server đang chạy, cần token)
```

---

## 2. Biến môi trường

Tất cả có thể đặt trong `application.properties` hoặc truyền qua biến môi trường hệ thống.

| Biến | Mặc định | Bắt buộc | Mô tả |
|---|---|---|---|
| `DB_USERNAME` | `m0tnamk09` | ✅ | Username MySQL |
| `DB_PASSWORD` | `1509` | ✅ | Password MySQL |
| `JWT_SECRET` | _(key mặc định)_ | ✅ | Secret key JWT, tối thiểu 32 ký tự |
| `CLAUDE_API_KEY` | _(rỗng)_ | ❌ | API key Claude AI (bỏ trống để tắt AI) |
| `PORT` | `8080` | ❌ | Cổng server |
| `UPLOAD_DIR` | `uploads/` | ❌ | Thư mục lưu file upload |

Ví dụ chạy với biến môi trường:

```bash
DB_USERNAME=root DB_PASSWORD=secret JWT_SECRET=my-super-secret-key-at-least-32-chars \
  ./mvnw spring-boot:run
```

---

## 3. Base URL

| Môi trường | Base URL |
|---|---|
| Local / Web | `http://localhost:8080` |
| Android Emulator | `http://10.0.2.2:8080` |
| iOS Simulator | `http://localhost:8080` |
| Production | _(cấu hình theo server thực tế)_ |

**CORS đã được phép cho:**
- `http://localhost:3000` (React Web)
- `http://localhost:4200` (Angular Web)
- `capacitor://localhost` (Capacitor mobile)
- `http://localhost` (chung)

---

## 4. Xác thực JWT

Mọi request cần xác thực phải gửi header:

```
Authorization: Bearer <accessToken>
```

| Token | Thời hạn |
|---|---|
| `accessToken` | 24 giờ |
| `refreshToken` | 7 ngày |

Khi nhận `401` → gọi `POST /api/v1/auth/refresh` với `refreshToken` để lấy cặp token mới.

**Các endpoint KHÔNG cần token:**
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/message/media/{filename}` (tải file media)

---

## 5. Flow tích hợp

```
┌─────────────────────────────────────────────────────┐
│ 1. Đăng ký / Đăng nhập                             │
│    POST /auth/register  hoặc  POST /auth/login      │
│    → { accessToken, refreshToken, userId, ... }     │
│                                                     │
│ 2. Lưu token                                        │
│    Android: SharedPreferences / EncryptedSharedPrefs│
│    iOS: Keychain                                    │
│    Web: localStorage / sessionStorage               │
│                                                     │
│ 3. Gọi REST API                                     │
│    Header: Authorization: Bearer <accessToken>      │
│                                                     │
│ 4. Kết nối WebSocket (realtime)                     │
│    ws://HOST/ws  (SockJS fallback)                  │
│    STOMP CONNECT Header: Authorization: Bearer ...  │
│                                                     │
│ 5. Làm mới token khi nhận 401                       │
│    POST /auth/refresh  → { accessToken, ... }       │
│    Lưu token mới → gọi lại request gốc             │
│                                                     │
│ 6. Đăng xuất                                        │
│    POST /auth/logout → xóa token → ngắt WebSocket  │
└─────────────────────────────────────────────────────┘
```

---

## 6. REST API

### Auth — `/api/v1/auth`

| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/auth/register` | ❌ | Đăng ký tài khoản |
| POST | `/api/v1/auth/login` | ❌ | Đăng nhập |
| POST | `/api/v1/auth/refresh` | ❌ | Làm mới token |
| POST | `/api/v1/auth/logout` | ✅ | Đăng xuất |

---

### User — `/api/v1/user`

| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/v1/user` | ✅ | Danh sách tất cả user (trừ bản thân) |
| GET | `/api/v1/user/me` | ✅ | Thông tin profile bản thân |
| PUT | `/api/v1/user/me` | ✅ | Cập nhật tên (firstName, lastName) |
| PATCH | `/api/v1/user/me/password` | ✅ | Đổi mật khẩu |
| POST | `/api/v1/user/me/avatar` | ✅ | Upload ảnh đại diện (`multipart/form-data`) |
| GET | `/api/v1/user/search?keyword=` | ✅ | Tìm kiếm user theo tên / email |

---

### Chat 1-1 — `/api/v1/chat`

| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/v1/chat` | ✅ | Danh sách cuộc trò chuyện của tôi |
| GET | `/api/v1/chat/{chatId}` | ✅ | Chi tiết một cuộc trò chuyện |
| POST | `/api/v1/chat/start/{otherUserId}` | ✅ | Bắt đầu / lấy chat với user khác |

---

### Tin nhắn 1-1 — `/api/v1/message`

| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/message` | ✅ | Gửi tin nhắn text |
| POST | `/api/v1/message/upload-media/{chatId}` | ✅ | Gửi file ảnh/video/audio/tài liệu |
| GET | `/api/v1/message/chat/{chatId}?page=0&size=30` | ✅ | Lấy tin nhắn (phân trang, mới nhất trước) |
| PATCH | `/api/v1/message/seen/{chatId}` | ✅ | Đánh dấu đã xem tất cả tin trong chat |
| DELETE | `/api/v1/message/{messageId}/recall` | ✅ | Thu hồi tin nhắn |
| GET | `/api/v1/message/media/{filename}` | ❌ | Tải xuống file media |

> **Upload media:** tối đa 50 MB/file. Hệ thống tự detect loại: `image/*` → `IMAGE` · `video/*` → `VIDEO` · `audio/*` → `AUDIO` · còn lại → `FILE`.

---

### Reaction tin nhắn 1-1 — `/api/v1/message`

| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/message/{messageId}/reactions?emoji=❤️` | ✅ | React (toggle: cùng emoji → xóa, khác → cập nhật) |
| DELETE | `/api/v1/message/{messageId}/reactions` | ✅ | Xóa reaction của bản thân |
| GET | `/api/v1/message/{messageId}/reactions` | ✅ | Lấy tất cả reactions |

---

### Nhóm chat — `/api/v1/group`

| Method | Endpoint | Auth | Admin nhóm | Mô tả |
|---|---|---|---|---|
| POST | `/api/v1/group` | ✅ | — | Tạo nhóm mới |
| GET | `/api/v1/group` | ✅ | — | Danh sách nhóm của tôi |
| GET | `/api/v1/group/{groupId}` | ✅ | — | Chi tiết nhóm |
| PUT | `/api/v1/group/{groupId}` | ✅ | ✅ | Cập nhật tên / mô tả nhóm |
| POST | `/api/v1/group/{groupId}/avatar` | ✅ | ✅ | Upload ảnh nhóm |
| POST | `/api/v1/group/{groupId}/members` | ✅ | ✅ | Thêm thành viên |
| DELETE | `/api/v1/group/{groupId}/members/{userId}` | ✅ | ✅ | Xóa thành viên |
| DELETE | `/api/v1/group/{groupId}/leave` | ✅ | — | Rời nhóm |
| POST | `/api/v1/group/{groupId}/messages` | ✅ | — | Gửi tin nhắn text vào nhóm |
| POST | `/api/v1/group/{groupId}/upload-media` | ✅ | — | Gửi media vào nhóm |
| GET | `/api/v1/group/{groupId}/messages?page=0&size=30` | ✅ | — | Lấy tin nhắn nhóm (phân trang) |
| DELETE | `/api/v1/group/{groupId}/messages/{messageId}/recall` | ✅ | — | Thu hồi tin nhắn nhóm |

---

### Reaction tin nhắn nhóm — `/api/v1/group-message`

| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/group-message/{messageId}/reactions?emoji=👍` | ✅ | React tin nhắn nhóm |
| DELETE | `/api/v1/group-message/{messageId}/reactions` | ✅ | Xóa reaction |
| GET | `/api/v1/group-message/{messageId}/reactions` | ✅ | Lấy tất cả reactions |

---

### AI Chatbot — `/api/v1/ai`

| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/ai/chat` | ✅ | Gửi tin nhắn cho Claude AI |
| GET | `/api/v1/ai/history?page=0&size=30` | ✅ | Lịch sử chat với AI (phân trang) |
| DELETE | `/api/v1/ai/history` | ✅ | Xóa toàn bộ lịch sử chat AI |

> Yêu cầu `CLAUDE_API_KEY` được cấu hình. Model: `claude-haiku-4-5-20251001`.

---

### Admin — `/api/v1/admin` _(chỉ tài khoản ROLE_ADMIN)_

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/admin/users?page=0&size=20` | Danh sách tất cả user (phân trang) |
| PATCH | `/api/v1/admin/users/{userId}/ban` | Khóa tài khoản |
| PATCH | `/api/v1/admin/users/{userId}/unban` | Mở khóa tài khoản |
| DELETE | `/api/v1/admin/users/{userId}` | Xóa tài khoản vĩnh viễn |
| PATCH | `/api/v1/admin/users/{userId}/promote` | Nâng lên ADMIN |
| PATCH | `/api/v1/admin/users/{userId}/demote` | Hạ xuống USER |
| GET | `/api/v1/admin/groups?page=0&size=20` | Danh sách tất cả nhóm (phân trang) |
| DELETE | `/api/v1/admin/groups/{groupId}` | Xóa nhóm |
| GET | `/api/v1/admin/stats` | Thống kê hệ thống |

---

## 7. Request / Response chi tiết

### Đăng ký

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@gmail.com",
  "password": "123456",
  "firstName": "Nguyen",
  "lastName": "Van A"
}
```

```json
// 201 Created
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "user@gmail.com",
  "firstName": "Nguyen",
  "lastName": "Van A",
  "online": false
}
```

---

### Đăng nhập

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@gmail.com",
  "password": "123456"
}
```

```json
// 200 OK — cùng cấu trúc AuthResponse như đăng ký
{
  "accessToken": "...",
  "refreshToken": "...",
  "userId": "uuid",
  "email": "user@gmail.com",
  "firstName": "Nguyen",
  "lastName": "Van A",
  "online": true
}
```

---

### Làm mới token

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

```json
// 200 OK — trả về cặp token mới
{
  "accessToken": "...",
  "refreshToken": "...",
  "userId": "uuid",
  "email": "user@gmail.com",
  "firstName": "Nguyen",
  "lastName": "Van A",
  "online": true
}
```

---

### Lấy profile bản thân

```http
GET /api/v1/user/me
Authorization: Bearer <accessToken>
```

```json
// 200 OK
{
  "id": "uuid",
  "firstName": "Nguyen",
  "lastName": "Van A",
  "email": "user@gmail.com",
  "avatarUrl": "/api/v1/message/media/abc123.jpg",
  "online": true,
  "lastSeen": "2026-04-03T10:00:00",
  "lastSeenText": "Đang hoạt động",
  "role": "USER",
  "banned": false
}
```

---

### Cập nhật tên

```http
PUT /api/v1/user/me
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "firstName": "Nguyen",
  "lastName": "Van B"
}
```

```json
// 200 OK — UserDto đã cập nhật
```

---

### Đổi mật khẩu

```http
PATCH /api/v1/user/me/password
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "currentPassword": "123456",
  "newPassword": "newpass123"
}
```

```
// 200 OK  (không có body)
```

---

### Upload ảnh đại diện

```http
POST /api/v1/user/me/avatar
Authorization: Bearer <accessToken>
Content-Type: multipart/form-data

file: <image file>
```

```json
// 200 OK — UserDto với avatarUrl đã cập nhật
{
  "id": "uuid",
  "avatarUrl": "/api/v1/message/media/avatar_uuid.jpg",
  ...
}
```

> Lấy ảnh: `GET /api/v1/message/media/{filename}` (không cần token)

---

### Tìm kiếm user

```http
GET /api/v1/user/search?keyword=nguyen
Authorization: Bearer <accessToken>
```

```json
// 200 OK
[
  {
    "id": "uuid",
    "firstName": "Nguyen",
    "lastName": "Van A",
    "email": "nguyen@gmail.com",
    "avatarUrl": null,
    "online": false,
    "lastSeenText": "2 giờ trước"
  }
]
```

---

### Bắt đầu / lấy chat 1-1

```http
POST /api/v1/chat/start/{otherUserId}
Authorization: Bearer <accessToken>
```

```json
// 200 OK hoặc 201 Created
{
  "id": "uuid",
  "user1Id": "uuid",
  "user2Id": "uuid",
  "chatName": "Nguyen Van A",
  "lastMessage": "Xin chào!",
  "lastMessageType": "TEXT",
  "lastMessageTime": "2026-04-03T10:00:00",
  "unreadCount": 0,
  "recipientOnline": true,
  "recipientLastSeenText": "Đang hoạt động"
}
```

---

### Gửi tin nhắn text (1-1)

```http
POST /api/v1/message
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "chatId": "uuid",
  "content": "Xin chào!",
  "type": "TEXT"
}
```

```
// 202 Accepted  (tin nhắn đẩy realtime qua WebSocket)
```

---

### Gửi media (1-1)

```http
POST /api/v1/message/upload-media/{chatId}
Authorization: Bearer <accessToken>
Content-Type: multipart/form-data

file: <file>
```

```
// 202 Accepted  (tin nhắn đẩy realtime qua WebSocket)
```

---

### Lấy tin nhắn 1-1 (phân trang)

```http
GET /api/v1/message/chat/{chatId}?page=0&size=30
Authorization: Bearer <accessToken>
```

```json
// 200 OK — mảng MessageDto
[
  {
    "id": "uuid",
    "chatId": "uuid",
    "senderId": "uuid",
    "receiverId": "uuid",
    "content": "Xin chào!",
    "type": "TEXT",
    "state": "SEEN",
    "createdAt": "2026-04-03T10:00:00",
    "mediaUrl": null,
    "deleted": false,
    "reactions": [
      {
        "id": "uuid",
        "userId": "uuid",
        "userFullName": "Nguyen Van A",
        "emoji": "❤️",
        "createdDate": "2026-04-03T10:01:00"
      }
    ]
  }
]
```

> `page=0` là trang mới nhất. `deleted: true` nghĩa là tin đã bị thu hồi, `content` sẽ là `null`.

---

### Thu hồi tin nhắn 1-1

```http
DELETE /api/v1/message/{messageId}/recall
Authorization: Bearer <accessToken>
```

```
// 200 OK  (tin nhắn bị xóa mềm, WebSocket broadcast thu hồi tới đối phương)
```

---

### Tạo nhóm

```http
POST /api/v1/group
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "name": "Nhóm học tập",
  "description": "Nhóm ôn thi cuối kỳ",
  "memberIds": ["uuid1", "uuid2"]
}
```

```json
// 201 Created
{
  "id": "uuid",
  "name": "Nhóm học tập",
  "description": "Nhóm ôn thi cuối kỳ",
  "avatarUrl": null,
  "createdById": "uuid",
  "memberCount": 3,
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

### Gửi tin nhắn nhóm

```http
POST /api/v1/group/{groupId}/messages
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "content": "Xin chào nhóm!"
}
```

```json
// 201 Created
{
  "id": "uuid",
  "content": "Xin chào nhóm!",
  "type": "TEXT",
  "groupId": "uuid",
  "senderId": "uuid",
  "senderName": "Nguyen Van A",
  "isMine": true,
  "createdDate": "2026-04-03T10:00:00",
  "deleted": false,
  "reactions": []
}
```

---

### Thêm thành viên vào nhóm _(admin nhóm)_

```http
POST /api/v1/group/{groupId}/members
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "userIds": ["uuid3", "uuid4"]
}
```

```json
// 200 OK — GroupDto với danh sách members đã cập nhật
```

---

### Chat với AI

```http
POST /api/v1/ai/chat
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "message": "Hôm nay thời tiết thế nào?"
}
```

```json
// 200 OK
{
  "id": "uuid",
  "role": "ASSISTANT",
  "content": "Xin lỗi, tôi không có thông tin thời tiết thực tế...",
  "createdDate": "2026-04-03T10:00:00"
}
```

---

### Thống kê Admin

```http
GET /api/v1/admin/stats
Authorization: Bearer <accessToken>
```

```json
// 200 OK
{
  "totalUsers": 120,
  "totalMessages": 5430,
  "totalGroups": 25,
  "onlineUsers": 18,
  "bannedUsers": 3,
  "dailyMessageCounts": [
    { "date": "2026-04-01", "count": 320 },
    { "date": "2026-04-02", "count": 280 }
  ],
  "topActiveUsers": [
    { "userId": "uuid", "fullName": "Nguyen Van A", "messageCount": 230 }
  ]
}
```

---

## 8. WebSocket Realtime

**URL kết nối:** `ws://HOST:8080/ws` (hỗ trợ SockJS fallback qua HTTP)  
**Protocol:** STOMP over SockJS  
**Authentication:** Gửi `Authorization` header trong frame STOMP CONNECT

---

### Kết nối — Web (JavaScript)

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  connectHeaders: {
    Authorization: 'Bearer ' + accessToken,
  },
  onConnect: () => {
    console.log('WebSocket connected');
    subscribeAll(client);
  },
  onDisconnect: () => {
    console.log('WebSocket disconnected');
  },
});

client.activate();
```

---

### Kết nối — Android (Kotlin, thư viện `krossbow` hoặc `StompProtocolAndroid`)

```kotlin
// Ví dụ với StompProtocolAndroid
val stompClient = Stomp.over(
    Stomp.ConnectionProvider.OKHTTP,
    "http://10.0.2.2:8080/ws/websocket"
)

val headers = listOf(StompHeader("Authorization", "Bearer $accessToken"))
stompClient.connect(headers)
```

---

### Kết nối — iOS (Swift, thư viện `StompClientLib`)

```swift
let url = URL(string: "http://localhost:8080/ws/websocket")!
stompClient.openSocketWithURLRequest(
    request: NSURLRequest(url: url),
    delegate: self,
    connectionHeaders: ["Authorization": "Bearer \(accessToken)"]
)
```

---

### Subscribe — Nhận dữ liệu realtime

Sau khi connect thành công, subscribe các topic sau:

```javascript
// 1. Tin nhắn 1-1 mới
client.subscribe('/user/queue/messages', (frame) => {
  const message = JSON.parse(frame.body); // MessageDto
});

// 2. Đối phương đã xem tin (1-1)
client.subscribe('/user/queue/seen', (frame) => {
  const { chatId } = JSON.parse(frame.body);
});

// 3. Tin nhắn bị thu hồi (1-1)
client.subscribe('/user/queue/message-recalled', (frame) => {
  const { messageId, chatId } = JSON.parse(frame.body);
});

// 4. Reaction thay đổi (1-1)
client.subscribe('/user/queue/reactions', (frame) => {
  const { messageId, chatId, reactions } = JSON.parse(frame.body);
  // reactions: ReactionDto[]
});

// 5. Tin nhắn nhóm mới
client.subscribe(`/topic/group/${groupId}`, (frame) => {
  const message = JSON.parse(frame.body); // GroupMessageDto
});

// 6. Typing indicator 1-1
client.subscribe(`/topic/chat/${chatId}/typing`, (frame) => {
  const { userId, isTyping } = JSON.parse(frame.body);
});

// 7. Typing indicator nhóm
client.subscribe(`/topic/group/${groupId}/typing`, (frame) => {
  const { userId, isTyping } = JSON.parse(frame.body);
});

// 8. Trạng thái online/offline của user
client.subscribe(`/topic/user/${userId}/status`, (frame) => {
  const { userId, isOnline } = JSON.parse(frame.body);
});
```

---

### Gửi — Publish từ client

```javascript
// Gửi typing indicator (1-1)
client.publish({
  destination: `/app/chat/${chatId}/typing`,
  body: JSON.stringify({ typing: true }),
});

// Gửi typing indicator (nhóm)
client.publish({
  destination: `/app/group/${groupId}/typing`,
  body: JSON.stringify({ typing: true }),
});
```

> **Lưu ý:** Gửi tin nhắn / media vẫn qua REST API, không qua WebSocket. WebSocket chỉ dùng để **nhận** thông báo realtime và gửi typing indicator.

---

### Tóm tắt topic

| Topic | Hướng | Dữ liệu | Mô tả |
|---|---|---|---|
| `/user/queue/messages` | Server → Client | `MessageDto` | Tin nhắn 1-1 mới |
| `/user/queue/seen` | Server → Client | `{ chatId }` | Đối phương đã xem |
| `/user/queue/message-recalled` | Server → Client | `{ messageId, chatId }` | Tin bị thu hồi |
| `/user/queue/reactions` | Server → Client | `{ messageId, chatId, reactions[] }` | Reaction thay đổi |
| `/topic/group/{groupId}` | Server → Client | `GroupMessageDto` | Tin nhắn nhóm mới |
| `/topic/chat/{chatId}/typing` | Server → Client | `{ userId, isTyping }` | Typing 1-1 |
| `/topic/group/{groupId}/typing` | Server → Client | `{ userId, isTyping }` | Typing nhóm |
| `/topic/user/{userId}/status` | Server → Client | `{ userId, isOnline }` | Trạng thái online |
| `/app/chat/{chatId}/typing` | Client → Server | `{ typing: boolean }` | Gửi typing 1-1 |
| `/app/group/{groupId}/typing` | Client → Server | `{ typing: boolean }` | Gửi typing nhóm |

---

## 9. Enums

### MessageType

| Giá trị | Mô tả |
|---|---|
| `TEXT` | Văn bản thông thường |
| `IMAGE` | Hình ảnh |
| `VIDEO` | Video |
| `AUDIO` | Âm thanh |
| `FILE` | Tài liệu / file khác |

### MessageState _(chỉ chat 1-1)_

| Giá trị | Mô tả |
|---|---|
| `SENT` | Đã gửi lên server |
| `DELIVERED` | Đã giao tới thiết bị nhận |
| `SEEN` | Đối phương đã xem |

### Role (user)

| Giá trị | Mô tả |
|---|---|
| `USER` | Người dùng thường |
| `ADMIN` | Quản trị viên hệ thống |

### Role AI message

| Giá trị | Mô tả |
|---|---|
| `USER` | Tin nhắn do người dùng gửi |
| `ASSISTANT` | Phản hồi từ Claude AI |

---

## 10. Mã lỗi

| HTTP | Ý nghĩa |
|---|---|
| `200` | Thành công |
| `201` | Tạo mới thành công |
| `202` | Đã nhận (xử lý async, ví dụ gửi tin nhắn) |
| `400` | Dữ liệu không hợp lệ |
| `401` | Chưa xác thực hoặc token hết hạn |
| `403` | Không có quyền thực hiện |
| `404` | Không tìm thấy tài nguyên |
| `409` | Xung đột (ví dụ: email đã tồn tại) |
| `500` | Lỗi hệ thống |

**Cấu trúc response lỗi:**

```json
// Lỗi thông thường
{
  "timestamp": "2026-04-03T10:00:00",
  "status": 401,
  "error": "Unauthorized"
}

// Lỗi validation (400)
{
  "timestamp": "2026-04-03T10:00:00",
  "status": 400,
  "error": "Validation failed",
  "details": {
    "email": "Email không hợp lệ",
    "password": "Mật khẩu tối thiểu 6 ký tự"
  }
}
```

---

## 11. Database Schema

| Bảng | Mô tả |
|---|---|
| `user` | Tài khoản người dùng |
| `chat` | Cuộc hội thoại 1-1 |
| `message` | Tin nhắn 1-1 (có xóa mềm) |
| `message_reaction` | Reaction tin nhắn 1-1 |
| `group` | Nhóm chat |
| `group_member` | Thành viên nhóm (có flag admin) |
| `group_message` | Tin nhắn nhóm (có xóa mềm) |
| `group_message_reaction` | Reaction tin nhắn nhóm |
| `ai_message` | Lịch sử chat với Claude AI |

Schema được quản lý bởi **Flyway** — tự động migrate khi server khởi động.  
Migration files: `src/main/resources/db/migration/V1__init_schema.sql` → `V5__...sql`

---

## 12. Tài khoản mẫu

Được tự động tạo khi khởi động lần đầu (database trống):

| Email | Password | Role |
|---|---|---|
| `admin@zalo.com` | `Admin@1234` | ADMIN |

Dữ liệu mẫu (chats, messages, groups) được import từ `src/main/resources/csvdata/` nếu tồn tại các file CSV.

---

*v5 — 04/2026*
