import React, { useEffect, useState } from 'react';
import { Search } from 'lucide-react';
import { getMyChats } from '../../api/chat';
import useChatStore from '../../store/chatStore';
import useAuthStore from '../../store/authStore';
import Avatar from '../common/Avatar';
import { formatDistanceToNow } from 'date-fns';
import { vi } from 'date-fns/locale';

function formatTime(dateStr) {
  if (!dateStr) return '';
  try {
    return formatDistanceToNow(new Date(dateStr), { addSuffix: false, locale: vi });
  } catch {
    return '';
  }
}

export default function ChatList() {
  const { chats, setChats, activeChatId, setActiveChatId } = useChatStore();
  const { auth } = useAuthStore();
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    getMyChats()
      .then((res) => setChats(res.data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const filtered = chats.filter((c) =>
    c.chatName?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="flex flex-col h-full">
      <div className="px-3 py-3 border-b border-gray-100">
        <div className="relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            placeholder="Tìm kiếm..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 bg-gray-100 rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-blue-200"
          />
        </div>
      </div>

      <div className="flex-1 overflow-y-auto">
        {loading && (
          <div className="flex items-center justify-center py-8">
            <div className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {!loading && filtered.length === 0 && (
          <div className="text-center py-8 text-gray-400 text-sm">
            Không có cuộc trò chuyện nào
          </div>
        )}

        {filtered.map((chat) => {
          const isActive = activeChatId === chat.id;
          const isMyMsg = chat.lastMessage && chat.user1Id === auth?.userId;

          return (
            <button
              key={chat.id}
              onClick={() => setActiveChatId(chat.id)}
              className={`flex items-center gap-3 px-4 py-3 w-full text-left hover:bg-gray-50 transition-colors ${
                isActive ? 'bg-blue-50 border-r-2 border-blue-500' : ''
              }`}
            >
              <Avatar
                src={chat.avatarUrl}
                name={chat.chatName}
                size={44}
                online={chat.recipientOnline}
              />
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <span className={`font-medium text-sm truncate ${isActive ? 'text-blue-600' : 'text-gray-800'}`}>
                    {chat.chatName || 'Người dùng'}
                  </span>
                  <span className="text-xs text-gray-400 ml-1 flex-shrink-0">
                    {formatTime(chat.lastMessageTime)}
                  </span>
                </div>
                <div className="flex items-center justify-between mt-0.5">
                  <span className="text-xs text-gray-500 truncate max-w-[150px]">
                    {chat.lastMessage
                      ? chat.lastMessageType === 'IMAGE'
                        ? '📷 Hình ảnh'
                        : chat.lastMessageType === 'VIDEO'
                        ? '🎥 Video'
                        : chat.lastMessageType === 'FILE'
                        ? '📎 Tệp đính kèm'
                        : chat.lastMessage
                      : 'Chưa có tin nhắn'}
                  </span>
                  {chat.unreadCount > 0 && (
                    <span className="ml-2 bg-blue-500 text-white text-xs rounded-full min-w-[18px] h-[18px] flex items-center justify-center px-1 flex-shrink-0">
                      {chat.unreadCount > 99 ? '99+' : chat.unreadCount}
                    </span>
                  )}
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
