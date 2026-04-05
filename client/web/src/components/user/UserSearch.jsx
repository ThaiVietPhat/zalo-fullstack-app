import React, { useState, useEffect } from 'react';
import { Search, MessageCircle } from 'lucide-react';
import { searchUsers, getAllUsers } from '../../api/user';
import { startChat } from '../../api/chat';
import useChatStore from '../../store/chatStore';
import Avatar from '../common/Avatar';
import toast from 'react-hot-toast';

export default function UserSearch() {
  const { setActiveChatId, setActiveTab, chats, setChats } = useChatStore();
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);
  const [debounced, setDebounced] = useState('');

  useEffect(() => {
    const t = setTimeout(() => setDebounced(search), 400);
    return () => clearTimeout(t);
  }, [search]);

  useEffect(() => {
    if (debounced.trim().length === 0) {
      // Load all users when no search
      getAllUsers()
        .then((res) => setUsers(res.data || []))
        .catch(() => {});
      return;
    }
    setLoading(true);
    searchUsers(debounced)
      .then((res) => setUsers(res.data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [debounced]);

  const handleStartChat = async (userId) => {
    try {
      const res = await startChat(userId);
      const chat = res.data;
      // Add to chats list if not exists
      setChats((prev) => {
        if (Array.isArray(prev)) {
          if (prev.find((c) => c.id === chat.id)) return prev;
          return [chat, ...prev];
        }
        return [chat];
      });
      setActiveChatId(chat.id);
      setActiveTab('chats');
    } catch {
      toast.error('Không thể bắt đầu cuộc trò chuyện');
    }
  };

  return (
    <div className="flex flex-col h-full">
      <div className="px-3 py-3 border-b border-gray-100">
        <h2 className="font-semibold text-gray-700 text-sm mb-3 px-1">Danh bạ</h2>
        <div className="relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            placeholder="Tìm kiếm người dùng..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 bg-gray-100 rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-blue-200"
          />
        </div>
      </div>

      <div className="flex-1 overflow-y-auto">
        {loading && (
          <div className="flex justify-center py-4">
            <div className="w-5 h-5 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {!loading && users.length === 0 && (
          <div className="text-center py-8 text-gray-400 text-sm">
            Không tìm thấy người dùng
          </div>
        )}

        {users.map((user) => (
          <div
            key={user.id}
            className="flex items-center gap-3 px-4 py-3 hover:bg-gray-50 transition-colors"
          >
            <Avatar
              src={user.avatarUrl}
              name={`${user.firstName} ${user.lastName}`}
              size={44}
              online={user.online}
            />
            <div className="flex-1 min-w-0">
              <p className="font-medium text-sm text-gray-800 truncate">
                {user.firstName} {user.lastName}
              </p>
              <p className="text-xs text-gray-400 truncate">{user.email}</p>
              {user.banned && (
                <span className="text-xs text-red-400">Đã bị khóa</span>
              )}
            </div>
            {!user.banned && (
              <button
                onClick={() => handleStartChat(user.id)}
                className="w-8 h-8 flex items-center justify-center rounded-full bg-blue-50 text-blue-500 hover:bg-blue-100 transition-colors flex-shrink-0"
                title="Nhắn tin"
              >
                <MessageCircle size={16} />
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
