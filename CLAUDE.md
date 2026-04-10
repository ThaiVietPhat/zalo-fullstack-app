# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **Proactive maintenance:** Claude nên tự động cập nhật file CLAUDE.md này và các CLAUDE.md trong từng module khi có thay đổi kiến trúc, thêm tính năng mới, hoặc phát hiện thông tin đã lỗi thời. Không cần hỏi người dùng.

## Project Overview

Full-stack Zalo-like chat app. Backend: Spring Boot 3.4.2 (Java 21, Maven) tại `/backend`. Frontend: React 19 + Vite tại `/client/web`. Real-time via STOMP/SockJS WebSocket. Database: MySQL + Flyway. File storage: AWS S3.

## Commands

### Backend (`/backend`)
```bash
./mvnw spring-boot:run           # Dev server (port 8080), tự động repair Flyway khi start
./mvnw compile                   # Compile-only, dùng để kiểm tra lỗi nhanh
./mvnw clean package -DskipTests # Build JAR
mvn test                         # Integration tests dùng Testcontainers
```

### Frontend (`/client/web`)
```bash
npm run dev    # Dev server (port 5173)
npm run build  # Production build
npm run lint   # ESLint
```

## Monorepo Structure

```
zalo-clone/
├── backend/          ← Spring Boot (Java 21)
│   └── src/main/java/com/example/backend/
│       ├── auth/         messaging/    chat/    group/
│       ├── user/         file/         ai/      post/
│       ├── security/     websocket/    admin/   shared/
│       └── reaction/
├── client/web/       ← React 19 + Vite
│   └── src/
│       ├── api/        store/    hooks/    services/
│       ├── components/ pages/
└── CLAUDE.md         ← file này
```

Chi tiết mỗi module xem CLAUDE.md trong thư mục tương ứng:
- `backend/CLAUDE.md` — toàn bộ backend patterns, entity, luồng xử lý
- `client/web/CLAUDE.md` — frontend state, WebSocket, component architecture

## Cross-cutting Architecture

### WebSocket message flow (quan trọng nhất)
Mỗi tin nhắn 1-1 backend gửi **song song 2 kênh**:
1. `/topic/chat/{chatId}` — broadcast, cả sender và receiver subscribe. Đây là **kênh đáng tin cậy**, dùng cho mọi UI update (lastMessage, unreadCount, sort lên đầu danh sách)
2. `/user/{email}/queue/messages` — personal queue. Chỉ dùng để **phát hiện chat mới** chưa có trong list

Group messages chỉ dùng `/topic/group/{groupId}`. Reaction events đi cùng topic nhưng phân biệt bằng: có `messageId + reactions`, không có `id`.

### Auth flow
JWT HS256 — access token 24h, refresh token 7 ngày. Claim `tokenVersion` để invalidate toàn bộ session cũ khi đăng nhập từ thiết bị mới. Frontend tự động refresh khi nhận 401.

### File storage
Files upload lên S3 với UUID key. Field `fileName` trong DB lưu tên gốc để hiển thị. Endpoint `/api/v1/message/media/{key}` hỗ trợ HTTP Range requests (206 Partial Content) để browser stream video/audio đúng cách.

## Environment Variables

| Variable | Default | Purpose |
|---|---|---|
| `DB_HOST` | `localhost` | MySQL host (production: AWS RDS) |
| `DB_NAME` | `zalo_clone` | Database name |
| `DB_USERNAME` | `m0tnamk09` | MySQL user |
| `DB_PASSWORD` | `1509` | MySQL password |
| `AWS_ACCESS_KEY_ID` | — | S3 credentials |
| `AWS_SECRET_ACCESS_KEY` | — | S3 credentials |
| `AWS_S3_BUCKET` | — | S3 bucket |
| `AWS_REGION` | `ap-southeast-1` | S3 region |
| `GEMINI_API_KEY` | — | Gemini AI |
| `BREVO_API_KEY` | — | Email service (verification) |
| `BREVO_SENDER_EMAIL` | — | From address |
| `VITE_WS_URL` | `origin:8080` | Frontend WebSocket base URL |

## Database Migrations

Flyway migrations: `backend/src/main/resources/db/migration/V{n}__{desc}.sql`

`FlywayConfig.java` tự động `repair()` rồi `migrate()` khi start — xử lý migration failed tự động. Khi viết migration mới: **không dùng `IF NOT EXISTS`** (AWS RDS MySQL không hỗ trợ cú pháp này trong `ALTER TABLE ADD COLUMN`).

## Key Dependencies
- Spring Boot 3.4.2, Java 21, MapStruct, JJWT 0.11.5, AWS SDK v2, Flyway 10, Hikari pool 5–15
- React 19, Vite, Zustand 5, `@stomp/stompjs` 7, SockJS, Axios, Tailwind CSS, date-fns, react-hot-toast, EmojiPicker
