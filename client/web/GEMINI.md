# GEMINI.md — Frontend (client/web)

React 19 + Vite + Zustand + STOMP WebSocket. Dev port: 5173, backend port: 8080.

> Cập nhật file này khi thêm tính năng, thay đổi state shape, hoặc thêm route/component quan trọng.

## App Structure

```
src/
├── App.jsx             — Routes: /login, /register, /forgot-password, /chat, /admin
├── main.jsx
├── api/                — Axios modules (mỗi domain 1 file)
│   ├── axios.js        — Instance với auth interceptor + auto-refresh
│   ├── auth.js         chat.js    message.js    group.js
│   ├── user.js         friendRequest.js    post.js    ai.js    admin.js
│   └── call.js         — saveCallSession(), getCallHistory(chatId)
├── store/
│   ├── authStore.js    — JWT tokens, userId, sessionReplaced flag
│   ├── chatStore.js    — Toàn bộ chat/group/message state
│   └── callStore.js    — activeCall, incomingCall, localStream, remoteStream, isMuted, isCameraOff   
├── hooks/
│   ├── useWebSocket.js — Connect WS, subscribe queues, subscribe inactive topics
│   └── useCallManager.js — WebRTC logic: startCall, acceptCall, rejectCall, endCall
├── services/
│   ├── websocket.js    — WebSocketService singleton
│   └── iceServers.js   — Google STUN + optional TURN từ env vars
├── pages/
│   ├── ChatPage.jsx    — Layout chính: Sidebar + ChatWindow/GroupWindow + call overlays
│   ├── LoginPage.jsx   RegisterPage.jsx   ForgotPasswordPage.jsx   AdminPage.jsx
└── components/
    ├── chat/           — ChatList, ChatWindow, MessageBubble, MessageInput, TypingIndicator, ChatInfoPanel
    ├── group/          — GroupList, GroupWindow (+ inline GroupMessageBubble), CreateGroupModal        
    ├── call/           — IncomingCallModal, ActiveCallBar, VideoCallWindow, CallBubble
    ├── layout/         — Sidebar, Header
    ├── user/           — UserSearch, UserSearchPanel, UserProfileModal
    ├── post/           — Feed components
    ├── ai/             — AI chat component
    └── common/         — Avatar, Modal, ProtectedRoute
```

## State Management (Zustand)

### authStore (`store/authStore.js`)
```js
auth: { accessToken, refreshToken, userId, email, role }  // persisted localStorage
sessionReplaced: bool   // true khi bị kick do đăng nhập nơi khác
```
- `logout()` → clear auth + disconnect WebSocket
- `setSessionReplaced()` → trigger UI thông báo và redirect

### callStore (`store/callStore.js`)
```js
activeCall: null   // { chatId, peerId, peerName, peerAvatar, callType, direction, status, startedAt }
incomingCall: null // CallSignalDto từ call-offer (fromUserId, fromUserName, fromUserAvatar, sdp, callType, chatId)
localStream: null  // MediaStream từ getUserMedia
remoteStream: null // MediaStream từ RTCPeerConnection.ontrack
isMuted: false
isCameraOff: false
```
- `direction`: `'outgoing'` | `'incoming'`
- `status`: `'ringing'` | `'connecting'` | `'active'`
- Chỉ **caller** gửi `saveCallSession()` khi kết thúc — tránh duplicate record

### chatStore (`store/chatStore.js`) — state quan trọng
```js
chats: []           // sorted by lastMessageTime, mới nhất lên đầu
groups: []          // tương tự
contacts: []        // bạn bè đã accept (UserDto[])
pendingRequests: [] // lời mời kết bạn đến
sentRequests: []    // lời mời đã gửi
activeChatId: null  // chat 1-1 đang mở
activeGroupId: null // group đang mở
activeTab: 'chats'  // 'chats'|'groups'|'search'|'feed'|'ai'|'contacts'
messages: {}        // { [chatId]: MessageDto[] }
groupMessages: {}   // { [groupId]: GroupMessageDto[] }
typingUsers: {}     // { 'chat_{id}': Set<userId>, 'group_{id}': Set<userId> }
onlineUsers: {}     // { [userId]: boolean }
```

**Sorting rule:** `updateChatLastMessage()` và `updateGroupLastMessage()` dùng prepend pattern — move chat lên đầu list, không dùng `.map()` in-place.

**Unread count:** `incrementUnread(chatId)` tăng +1, `clearUnread(chatId)` reset về 0 (khi mở chat hoặc nhận seen event).

## WebSocket Architecture

### WebSocketService singleton (`services/websocket.js`)
- `this.handlers` — `{ destination: callback }` — **không bao giờ bị xóa khi reconnect**
- `this.stompSubs` — active STOMP subscriptions (bị clear khi disconnect)
- Khi reconnect: re-subscribe tất cả từ `this.handlers` tự động
- `subscribe(dest, cb)`: overwrite handler cũ nếu đã có → tránh duplicate
- `unsubscribe(dest)`: xóa khỏi cả `handlers` và `stompSubs`
- `onReconnect(cb)` / `offReconnect(cb)`: đăng ký callback chạy sau mỗi lần reconnect (không phải connect lần đầu)

### useWebSocket hook (`hooks/useWebSocket.js`)
Được gọi tại `ChatPage` (root của chat layout). Manages:
1. **Kết nối WS** khi có `auth.accessToken`
2. **Personal queues**: `/user/queue/messages`, `/user/queue/seen`, `/user/queue/message-recalled`, `/user/queue/reactions`, `/user/queue/friend-request`, `/user/queue/friend-request-accepted`, `/user/queue/force-logout`    
3. **Inactive chat topics**: subscribe tất cả chat trong list (trừ `activeChatId`) để cập nhật sidebar realtime

**Quan trọng:** `/user/queue/messages` chỉ dùng để phát hiện **chat mới chưa có trong list** (fetch detail + prepend). Không gọi `updateChatLastMessage`/`incrementUnread` tại đây vì `/topic/chat/{id}` đã làm — tránh double-count.

### useCallManager hook (`hooks/useCallManager.js`)
Mount tại `ChatPage`. Subscribe `/topic/call/{auth.userId}` — nhận signal từ peer.
- `handleSignalRef` pattern: ref chứa handler mới nhất, subscription chỉ tạo 1 lần (không teardown khi handler thay đổi)
- `pcRef` — RTCPeerConnection, `localStreamRef` — MediaStream, `callStartTimeRef` — timestamp bắt đầu gọi
- `createPeerConnection()` gắn `ontrack` → `setRemoteStream`, `onconnectionstatechange` → trigger `handlePeerDisconnect`
- Call end: nếu `direction === 'outgoing'` → `saveCallSession()` (REST). Peer disconnect → tương tự   

### Call components
- **`IncomingCallModal`** — hiển thị khi `incomingCall !== null`, phát `/sounds/ringtone.mp3` (AudioContext), nút accept/reject
- **`ActiveCallBar`** — thanh xanh thu nhỏ ở top khi `activeCall !== null`, hiện timer, nút mute/camera/end
- **`VideoCallWindow`** — fullscreen khi `callType === 'VIDEO'` và `activeCall.status === 'active'`
- **`CallBubble`** — render trong ChatWindow timeline cho `_isCall` items, màu xanh (ENDED) / đỏ (MISSED/REJECTED)

### ChatWindow timeline (call + message merge)
`buildTimeline(messages, callHistory)` merge và sort theo `_time`. Mỗi item có `_isCall: true/false`.       
Auto-refetch call history 1s sau khi `activeCall` chuyển từ có giá trị → null.

### ChatWindow subscription (riêng biệt)
`ChatWindow.jsx` có local `subscribeToChat()` / `unsubscribeFromChat()` **riêng** — KHÔNG dùng cái từ `useWebSocket`. Khi mở chat: subscribe `/topic/chat/{id}`. Khi đóng: unsubscribe → xóa khỏi `wsService.handlers`. `useWebSocket` effect sẽ re-subscribe khi `activeChatId` thay đổi.

## API Layer (`api/`)

`axios.js` — base instance:
- Request interceptor: attach `Authorization: Bearer {token}`
- Response interceptor: nhận 401 → tự động call refresh → retry request gốc
- Queue các request đang chờ trong lúc refresh để không gọi refresh nhiều lần

Mỗi file API (vd `message.js`) export các function tương ứng với backend endpoints.

## Component Notes

### MessageBubble (`components/chat/MessageBubble.jsx`)
- Render theo `message.type`: TEXT | IMAGE | VIDEO | FILE | AUDIO
- FILE: hiển thị `message.fileName` (tên gốc) hoặc fallback UUID. Click → mở S3 URL tab mới. Nút ⬇ → `downloadFile()` (fetch qua backend với auth header)
- VIDEO: `<video controls>` với `src` = S3 URL trực tiếp (Range requests được S3 handle natively). Có fullscreen modal
- `downloadFile()` function: fetch qua `/api/v1/message/media/{key}?download=true` với auth header, tạo blob URL

### GroupWindow (`components/group/GroupWindow.jsx`)
- Tự chứa inline `GroupMessageBubble` (không import từ ngoài)
- Tự chứa `downloadFile()` function riêng (không import từ MessageBubble)
- Rất lớn — chứa cả group management UI (add/remove member, đổi tên, avatar, dissolve)

### Sidebar (`components/layout/Sidebar.jsx`)
- Tabs: Tin nhắn | Nhóm | Tìm kiếm | Feed | AI | Danh bạ
- `activeTab` trong chatStore điều khiển nội dung hiển thị

## Tailwind Patterns
- Blue primary: `bg-[#0068ff]` (màu Zalo)
- Bubble mine: `bg-[#0068ff] text-white rounded-br-sm`
- Bubble other: `bg-white text-gray-800 rounded-bl-sm border border-gray-100`
- Avatar online dot: `online` prop trên `<Avatar>`