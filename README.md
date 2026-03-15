
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

## 🖥️ Setup Client — Web (React / Angular / Vue)

### Bước 1 — Cài thư viện
```bash
# Axios để gọi REST API
npm install axios

# WebSocket STOMP
npm install @stomp/stompjs sockjs-client
```

### Bước 2 — Cấu hình Base URL
Tạo file `src/config/api.js`:
```javascript
export const BASE_URL = 'http://localhost:8080';
export const WS_URL   = 'http://localhost:8080/ws';
```

### Bước 3 — Axios với auto-attach token
Tạo file `src/config/axiosInstance.js`:
```javascript
import axios from 'axios';
import { BASE_URL } from './api';

const api = axios.create({ baseURL: BASE_URL });

// Tự gắn token vào mọi request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Tự refresh token khi hết hạn (401)
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const { data } = await axios.post(`${BASE_URL}/api/v1/auth/refresh`, { refreshToken });
          localStorage.setItem('accessToken', data.accessToken);
          error.config.headers.Authorization = `Bearer ${data.accessToken}`;
          return axios(error.config); // retry request cũ
        } catch {
          localStorage.clear();
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;
```

### Bước 4 — Kết nối WebSocket
```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WS_URL } from './api';

let stompClient = null;

export function connectWebSocket(myEmail, onMessage, onGroupMessage, onUserStatus) {
  const accessToken = localStorage.getItem('accessToken');

  stompClient = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    connectHeaders: { Authorization: `Bearer ${accessToken}` },
    onConnect: () => {
      // Tin nhắn 1-1
      stompClient.subscribe(`/user/${myEmail}/queue/messages`, (msg) => {
        onMessage(JSON.parse(msg.body));
      });

      // Trạng thái online/offline
      stompClient.subscribe('/topic/user-status', (msg) => {
        onUserStatus(JSON.parse(msg.body));
      });
    },
  });

  stompClient.activate();
}

// Subscribe thêm khi mở nhóm
export function subscribeGroup(groupId, onGroupMessage) {
  stompClient?.subscribe(`/topic/group/${groupId}`, (msg) => {
    onGroupMessage(JSON.parse(msg.body));
  });
}

export function disconnectWebSocket() {
  stompClient?.deactivate();
}
```

---

## 📱 Setup Client — Mobile Flutter

### Bước 1 — Thêm dependencies vào `pubspec.yaml`
```yaml
dependencies:
  dio: ^5.4.0                    # HTTP client
  stomp_dart_client: ^1.0.0      # WebSocket STOMP
  shared_preferences: ^2.2.0     # Lưu token
  flutter_secure_storage: ^9.0.0 # Lưu token bảo mật
```

### Bước 2 — Cấu hình Base URL
Tạo file `lib/core/config/app_config.dart`:
```dart
class AppConfig {
  static const String baseUrl  = 'http://10.0.2.2:8080'; // Android emulator
  // static const String baseUrl = 'http://localhost:8080'; // iOS simulator
  static const String wsUrl    = 'ws://10.0.2.2:8080/ws/websocket';
}
```

### Bước 3 — Dio với auto-attach token
Tạo file `lib/core/network/dio_client.dart`:
```dart
import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../config/app_config.dart';

class DioClient {
  static Dio createDio() {
    final dio = Dio(BaseOptions(baseUrl: AppConfig.baseUrl));

    // Tự gắn token
    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        final prefs = await SharedPreferences.getInstance();
        final token = prefs.getString('accessToken');
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        return handler.next(options);
      },
      // Tự refresh khi 401
      onError: (error, handler) async {
        if (error.response?.statusCode == 401) {
          final prefs = await SharedPreferences.getInstance();
          final refreshToken = prefs.getString('refreshToken');
          if (refreshToken != null) {
            try {
              final res = await Dio().post(
                '${AppConfig.baseUrl}/api/v1/auth/refresh',
                data: {'refreshToken': refreshToken},
              );
              final newToken = res.data['accessToken'];
              await prefs.setString('accessToken', newToken);
              error.requestOptions.headers['Authorization'] = 'Bearer $newToken';
              final retryRes = await dio.fetch(error.requestOptions);
              return handler.resolve(retryRes);
            } catch (_) {
              await prefs.clear();
              // navigate to login
            }
          }
        }
        return handler.next(error);
      },
    ));

    return dio;
  }
}
```

### Bước 4 — Kết nối WebSocket
```dart
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../config/app_config.dart';

class WebSocketService {
  late StompClient _client;

  Future<void> connect({
    required String myEmail,
    required Function(Map) onMessage,
    required Function(Map) onUserStatus,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('accessToken') ?? '';

    _client = StompClient(
      config: StompConfig(
        url: AppConfig.wsUrl,
        stompConnectHeaders: {'Authorization': 'Bearer $token'},
        onConnect: (frame) {
          // Tin nhắn 1-1
          _client.subscribe(
            destination: '/user/$myEmail/queue/messages',
            callback: (frame) => onMessage(jsonDecode(frame.body!)),
          );
          // Trạng thái online/offline
          _client.subscribe(
            destination: '/topic/user-status',
            callback: (frame) => onUserStatus(jsonDecode(frame.body!)),
          );
        },
      ),
    );
    _client.activate();
  }

  // Subscribe khi mở nhóm
  void subscribeGroup(String groupId, Function(Map) onGroupMessage) {
    _client.subscribe(
      destination: '/topic/group/$groupId',
      callback: (frame) => onGroupMessage(jsonDecode(frame.body!)),
    );
  }

  void disconnect() => _client.deactivate();
}
```

### Bước 5 — Lưu token sau login
```dart
Future<void> saveTokens(Map<String, dynamic> authResponse) async {
  final prefs = await SharedPreferences.getInstance();
  await prefs.setString('accessToken',  authResponse['accessToken']);
  await prefs.setString('refreshToken', authResponse['refreshToken']);
  await prefs.setString('userId',       authResponse['userId']);
  await prefs.setString('email',        authResponse['email']);
}
```

---

## 📱 Setup Client — Mobile Android (Kotlin)

### Bước 1 — Thêm dependencies vào `build.gradle`
```gradle
dependencies {
    // HTTP
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

    // WebSocket STOMP
    implementation 'ua.naiksoftware:stomp-client-jvm:1.6.6'
    implementation 'io.reactivex.rxjava2:rxjava:2.2.21'
    implementation 'io.reactivex.rxjava2:rxandroid:2.1.1'
}
```

### Bước 2 — Retrofit với token interceptor
```kotlin
object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    fun create(context: Context): ApiService {
        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

        val okhttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = prefs.getString("accessToken", null)
                val request = if (token != null)
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                else chain.request()
                chain.proceed(request)
            }.build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okhttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
```

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