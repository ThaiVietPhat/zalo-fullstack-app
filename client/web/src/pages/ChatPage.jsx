import React, { useEffect } from 'react';
import Sidebar from '../components/layout/Sidebar';
import ChatWindow from '../components/chat/ChatWindow';
import GroupWindow from '../components/group/GroupWindow';
import useChatStore from '../store/chatStore';
import useAuthStore from '../store/authStore';
import wsService from '../services/websocket';
import { useWebSocket } from '../hooks/useWebSocket';

export default function ChatPage() {
  const { activeTab, activeChatId, activeGroupId } = useChatStore();
  const { auth } = useAuthStore();

  // Initialize WebSocket connection
  useWebSocket();

  useEffect(() => {
    if (auth?.accessToken && !wsService.isConnected()) {
      wsService.connect(auth.accessToken);
    }
  }, [auth?.accessToken]);

  const showChatWindow = activeTab === 'chats' && activeChatId;
  const showGroupWindow = activeTab === 'groups' && activeGroupId;
  const showEmpty = !showChatWindow && !showGroupWindow && activeTab !== 'ai' && activeTab !== 'contacts';

  return (
    <div className="h-screen flex bg-gray-100 overflow-hidden">
      {/* Sidebar - 320px fixed */}
      <div className="w-80 flex-shrink-0 h-full border-r border-gray-200 shadow-sm">
        <Sidebar />
      </div>

      {/* Main content area */}
      <div className="flex-1 flex flex-col h-full overflow-hidden">
        {showChatWindow && <ChatWindow key={activeChatId} />}
        {showGroupWindow && <GroupWindow key={activeGroupId} />}
        {showEmpty && (
          <div className="flex-1 flex items-center justify-center bg-gray-50">
            <div className="text-center">
              <div className="text-7xl mb-6">💬</div>
              <h2 className="text-2xl font-bold text-gray-700 mb-2">Zalo Clone</h2>
              <p className="text-gray-400 text-sm">
                Chọn một cuộc trò chuyện hoặc nhóm để bắt đầu nhắn tin
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
