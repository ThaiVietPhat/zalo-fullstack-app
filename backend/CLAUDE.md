# CLAUDE.md — Backend

Spring Boot 3.4.2, Java 21, Maven. Production DB: AWS RDS MySQL. File storage: AWS S3.

> Cập nhật file này khi thêm module mới, thay đổi pattern, hoặc phát hiện thông tin lỗi thời.

## Package Map

```
com.example.backend/
├── auth/           — Đăng ký, đăng nhập, xác thực email, đổi mật khẩu
├── security/       — JWT filter, Spring Security config, BCrypt
├── user/           — Profile, tìm kiếm user, friend request (PENDING/ACCEPTED/REJECTED)
├── chat/           — Chat 1-1: entity Chat, controller, service
├── messaging/      — Message entity, MessageDto, mapper, enums, reactions, recall, soft-delete
├── group/          — Group chat: Group, GroupMember, GroupMessage, roles (ADMIN/MEMBER)
├── file/           — FileStorageService: upload/download S3, Range request support
├── websocket/      — WebSocketConfig: STOMP endpoint /ws, JWT auth interceptor
├── ai/             — AI chat với Gemini API
├── post/           — Feed: Post entity, PostDto, create/read posts
├── reaction/       — ReactionDto dùng chung cho message và group message
├── admin/          — Admin stats endpoint
└── shared/         — BaseAuditingEntity, GlobalExceptionHandler, FlywayConfig, DataSeeder
```

## Patterns & Rules

### Entity conventions
- Tất cả entity kế thừa `BaseAuditingEntity` → có `createdDate`, `lastModifiedDate` tự động
- ID dùng `@UuidGenerator` + `@JdbcTypeCode(SqlTypes.CHAR)` — UUID lưu dạng CHAR(36) trong MySQL
- JPA mode = `validate` — **Flyway owns the schema**. Không bao giờ dùng `spring.jpa.hibernate.ddl-auto=update`

### Flyway
- Migration files: `V{n}__{description}.sql` trong `src/main/resources/db/migration/`
- **Không dùng `IF NOT EXISTS`** trong `ALTER TABLE ADD COLUMN` — AWS RDS không hỗ trợ
- `FlywayConfig.java` tự `repair()` + `migrate()` khi startup — migration failed tự phục hồi
- Khi migration thất bại ở local: xóa record failed trong `flyway_schema_history` rồi restart

### MapStruct mapper
- `MessageMapper` dùng MapStruct — fields cùng tên tự map, chỉ cần `@Mapping` khi tên khác nhau
- `toMediaUrl()` trong mapper: nếu content là URL đầy đủ (bắt đầu `http`) → dùng nguyên; nếu là key → thêm prefix `/api/v1/message/media/`
- `GroupService.toMessageDto()` là private method, không dùng MapStruct

### Security
- `JwtAuthenticationFilter` validate JWT trên mọi HTTP request (trừ whitelist: `/auth/**`, `/ws/**`, `/media/**`)
- WebSocket CONNECT: JWT validate trong `WebSocketConfig` ChannelInterceptor, principal = email
- `tokenVersion` trong JWT claim: nếu version mismatch với DB → reject. Tăng version khi login mới → force logout thiết bị cũ

### File/Media
- Upload → `FileStorageService.saveFile()` → lưu với UUID key → trả public S3 URL
- Field `fileName` trong Message/GroupMessage lưu tên file gốc (`file.getOriginalFilename()`)
- `GET /api/v1/message/media/{key}` hỗ trợ HTTP Range (206 Partial Content) — **bắt buộc** để browser stream video có audio
- Allowed MIME types: ảnh, video, audio, PDF, Office docs, zip, txt (max 50MB)

### WebSocket destinations
- `/app/chat/{id}/typing` — client gửi typing event
- `/app/group/{id}/typing` — client gửi typing event nhóm
- `/topic/chat/{id}` — broadcast tin nhắn 1-1
- `/topic/chat/{id}/typing` — broadcast typing indicator
- `/topic/group/{id}` — broadcast tin nhắn nhóm (và reaction group)
- `/topic/group/{id}/typing` — broadcast typing nhóm
- `/topic/user/{id}/status` — online/offline status
- `/user/queue/messages` — personal: tin nhắn mới cho receiver
- `/user/queue/seen` — personal: mark seen notification
- `/user/queue/message-recalled` — personal: thu hồi tin nhắn
- `/user/queue/reactions` — personal: reaction update
- `/user/queue/friend-request` — personal: lời mời kết bạn đến
- `/user/queue/friend-request-accepted` — personal: chấp nhận kết bạn
- `/user/queue/force-logout` — personal: kick session cũ

### Notification pattern
1-1 message: backend gửi **cả hai**: `sendChatBroadcast()` → `/topic/chat/{id}` VÀ `sendMessageNotification()` → `/user/{email}/queue/messages`. Frontend xử lý cả hai nhưng chỉ `/topic` mới đáng tin cho UI update.

## API Endpoints tổng hợp

```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/verify-email
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password

GET    /api/v1/user/me
PUT    /api/v1/user/me
GET    /api/v1/user/search?query=
GET    /api/v1/user/{id}

GET    /api/v1/chat             — danh sách chat của tôi
GET    /api/v1/chat/{id}        — chi tiết chat (tên, avatar, online status)
DELETE /api/v1/chat/{id}        — xóa chat (soft delete)

POST   /api/v1/message          — gửi tin nhắn text
GET    /api/v1/message/chat/{chatId}?page=0&size=30
POST   /api/v1/message/upload-media/{chatId}
PATCH  /api/v1/message/seen/{chatId}
DELETE /api/v1/message/{id}/recall
DELETE /api/v1/message/{id}     — xóa phía mình
GET    /api/v1/message/media/{key}?download=true  — stream/download file

POST   /api/v1/group
GET    /api/v1/group            — nhóm của tôi
GET    /api/v1/group/{id}
POST   /api/v1/group/{id}/message
POST   /api/v1/group/{id}/upload-media
...

POST   /api/v1/friend-request
GET    /api/v1/friend-request/pending
GET    /api/v1/friend-request/sent
PUT    /api/v1/friend-request/{id}/accept
PUT    /api/v1/friend-request/{id}/reject
GET    /api/v1/friend-request/contacts   — danh sách bạn bè (ACCEPTED)

POST   /api/v1/reaction/message/{id}
DELETE /api/v1/reaction/message/{id}
POST   /api/v1/reaction/group-message/{id}
DELETE /api/v1/reaction/group-message/{id}

GET    /api/v1/post
POST   /api/v1/post

GET    /api/v1/admin/stats
```
