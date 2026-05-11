# GEMINI.md - Zalo Clone Project Overview

This file provides guidance and project context for the AI agent working on this repository.

## Project Overview

Full-stack Zalo-like chat app. 
- **Backend**: Spring Boot 3.4.2 (Java 21, Maven) tại `/backend`. 
- **Frontend**: React 19 + Vite tại `/client/web`. 
- **Mobile**: Kotlin + Jetpack Compose tại `/client/mobile`.
- **Real-time**: via STOMP/SockJS WebSocket. 
- **Database**: MySQL + Flyway. 
- **File storage**: Cloudflare R2 (S3-compatible).

## Production Stack
- **Backend**: Railway (Spring Boot, Dockerfile, `backend/railway.toml`)
- **Database**: Railway MySQL addon — cùng internal network với backend, latency < 1ms
- **File storage**: Cloudflare R2 — S3-compatible, miễn phí 10GB, không egress fee
- **Frontend**: Vercel — `https://zalo-fullstack-app.vercel.app`
- **Env vars mẫu**: `railway-env.example` tại root

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

### Mobile (`/client/mobile`)
```bash
.\gradlew.bat assembleDebug      # Build APK
```

## Monorepo Structure

```
zalo-clone/
├── backend/          ← Spring Boot (Java 21)
│   └── src/main/java/com/example/backend/
│       ├── auth/         messaging/    chat/    group/
│       ├── user/         file/         ai/      post/
│       ├── security/     websocket/    admin/   shared/
│       ├── reaction/
│       └── call/         ← WebRTC signaling + call history
├── client/
│   ├── web/          ← React 19 + Vite
│   │   └── src/
│   │       ├── api/        store/    hooks/    services/
│   │       ├── components/ pages/
│   │       └── public/sounds/ringtone.mp3
│   └── mobile/       ← Kotlin + Jetpack Compose
│       └── app/src/main/java/com/example/zalo/
│           ├── data/       di/       domain/
│           ├── ui/         util/
│           └── MainActivity.kt
└── GEMINI.md         ← file này
```

Chi tiết mỗi module xem GEMINI.md trong thư mục tương ứng:
- `backend/GEMINI.md` — toàn bộ backend patterns, entity, luồng xử lý
- `client/web/GEMINI.md` — frontend state, WebSocket, component architecture

## Cross-cutting Architecture

### WebSocket message flow (quan trọng nhất)
Mỗi tin nhắn 1-1 backend gửi **song song 2 kênh**:
1. `/topic/chat/{chatId}` — broadcast, cả sender và receiver subscribe. Đây là **kênh đáng tin cậy**, dùng cho mọi UI update (lastMessage, unreadCount, sort lên đầu danh sách)
2. `/user/{email}/queue/messages` — personal queue. Chỉ dùng để **phát hiện chat mới** chưa có trong list

Group messages chỉ dùng `/topic/group/{groupId}`. Reaction events đi cùng topic nhưng phân biệt bằng: có `messageId + reactions`, không có `id`.

### WebRTC Call flow
Signaling qua STOMP WebSocket — tái dùng infrastructure hiện có:
- Client → `/app/call/signal` (publish) → backend relay → `/topic/call/{targetUserId}` (subscribe)        
- Signal types: `call-offer`, `call-answer`, `call-reject`, `call-cancel`, `call-end`, `ice-candidate`
- ICE servers: Google STUN (free) + optional TURN qua env vars (`VITE_TURN_URL`, `VITE_TURN_USERNAME`, `VITE_TURN_CREDENTIAL`)
- Call history lưu riêng trong bảng `call_session` (không mix vào `message`), hiển thị trong timeline qua `CallBubble`
- Chỉ caller (initiator) gửi `POST /api/v1/calls` để lưu lịch sử — tránh duplicate

### Auth flow
JWT HS256 — access token 24h, refresh token 7 ngày. Claim `tokenVersion` để invalidate toàn bộ session cũ khi đăng nhập từ thiết bị mới. Frontend/Mobile tự động refresh khi nhận 401 hoặc clear token và redirect về Login.

### File storage
Files upload lên S3 với UUID key. Field `fileName` trong DB lưu tên gốc để hiển thị. Endpoint `/api/v1/message/media/{key}` hỗ trợ HTTP Range requests (206 Partial Content) để browser stream video/audio đúng cách.

## Environment Variables

| Variable | Default | Purpose |
|---|---|---|
| `DB_HOST` | `localhost` | MySQL host (production: Railway MySQL addon) |
| `DB_NAME` | `zalo_clone` | Database name |
| `DB_USERNAME` | `m0tnamk09` | MySQL user |
| `DB_PASSWORD` | `1509` | MySQL password |
| `AWS_ACCESS_KEY_ID` | — | S3 credentials |
| `AWS_SECRET_ACCESS_KEY` | — | S3 credentials |
| `AWS_S3_BUCKET` | — | S3 bucket |
| `AWS_REGION` | `ap-southeast-1` | S3 region |
| `GROQ_API_KEY` | — | Groq AI (llama-3.1-8b-instant, OpenAI-compatible) |
| `BREVO_API_KEY` | — | Email service (verification) |
| `BREVO_SENDER_EMAIL` | — | From address |
| `VITE_WS_URL` | `origin:8080` | Frontend WebSocket base URL |
| `VITE_TURN_URL` | — | TURN server URL (optional, cho WebRTC NAT traversal) |
| `VITE_TURN_USERNAME` | — | TURN credentials |
| `VITE_TURN_CREDENTIAL` | — | TURN credentials |

## Database Migrations

Flyway migrations: `backend/src/main/resources/db/migration/V{n}__{desc}.sql`

`FlywayConfig.java` tự động `repair()` rồi `migrate()` khi start — xử lý migration failed tự động. Railway MySQL là MySQL chuẩn — **có thể dùng `IF NOT EXISTS`** trong `ALTER TABLE ADD COLUMN`.     

## Key Dependencies
- Spring Boot 3.4.2, Java 21, MapStruct, JJWT 0.11.5, AWS SDK v2 (trỏ sang R2), Flyway 10, Hikari pool 2–5 (production)
- React 19, Vite, Zustand 5, `@stomp/stompjs` 7, SockJS, Axios, Tailwind CSS, date-fns, react-hot-toast, EmojiPicker
- Kotlin 1.9.24, Jetpack Compose, Retrofit, OkHttp, Dagger Hilt, Room, Krossbow STOMP, WebRTC, Coil
