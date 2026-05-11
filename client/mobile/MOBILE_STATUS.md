# Kế hoạch & Trạng thái Mobile App (Kotlin) - HOÀN THÀNH CƠ BẢN

## 1. Cấu trúc Dự án
- **Kiến trúc:** Clean Architecture (Data, Domain, UI).
- **Tech Stack:** Kotlin, Jetpack Compose, Hilt, Retrofit, OkHttp, Room, Krossbow (STOMP).
- **Network:** Kết nối Railway Backend thực tế.

## 2. Các tính năng đã hoàn thành (Feature Parity với Web)
- [x] **Auth:** Đăng ký, Đăng nhập, Quản lý Token (Persistent).
- [x] **Real-time Messaging:** Nhắn tin 1-1 và Nhóm thời gian thực (WebSocket).
- [x] **Trạng thái:** Typing indicator cho cả 1-1 và Nhóm.
- [x] **AI Features:** Gợi ý trả lời thông minh (Smart Reply) và Tóm tắt hội thoại (Summarize).
- [x] **Media:** Tích hợp bộ chọn file và Upload media lên R2 qua Backend.
- [x] **Home UI:** Giao diện tab chuẩn Zalo (Tin nhắn, Danh bạ, Nhóm).
- [x] **Navigation:** Luồng điều hướng hoàn chỉnh giữa các màn hình.

## 3. Ghi chú Kỹ thuật
- Toàn bộ DTO được đồng bộ với Backend Spring Boot.
- Sử dụng WebSocketManager tập trung để quản lý mọi kết nối thời gian thực.
- Giao diện sử dụng Jetpack Compose với theme màu "Zalo Blue" (0xFF0068FF).
