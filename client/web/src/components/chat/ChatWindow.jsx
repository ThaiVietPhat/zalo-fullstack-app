import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Phone, Video, Info, ChevronLeft } from 'lucide-react';
import { getChatDetail } from '../../api/chat';
import { getMessages, sendMessage, uploadMedia, markSeen } from '../../api/message';
import useChatStore from '../../store/chatStore';
import useAuthStore from '../../store/authStore';
import wsService from '../../services/websocket';
import Avatar from '../common/Avatar';
import MessageBubble from './MessageBubble';
import MessageInput from './MessageInput';
import TypingIndicator from './TypingIndicator';
import ChatInfoPanel from './ChatInfoPanel';
import toast from 'react-hot-toast';

export default function ChatWindow() {
  const { activeChatId, messages, typingUsers, setMessages, prependMessages, addMessage, updateChatLastMessage, updateChat, setActiveChatId, clearUnread, setTyping } = useChatStore();
  const { auth } = useAuthStore();

  const subscribeToChat = (chatId) => {
    // Subscribe to message delivery topic (broadcast, same pattern as group messages)
    wsService.subscribe(`/topic/chat/${chatId}`, (message) => {
      addMessage(chatId, message);
      updateChatLastMessage(chatId, message);
      // Chat đang mở, nhận message từ người khác → mark seen ngay để sender cập nhật real-time
      if (message.senderId !== auth?.userId) {
        markSeen(chatId).catch(() => {});
      }
    });
    // Subscribe to typing indicator
    wsService.subscribe(`/topic/chat/${chatId}/typing`, (data) => {
      const { userId, isTyping } = data;
      if (userId === auth?.userId) return;
      setTyping(`chat_${chatId}`, userId, isTyping);
      if (isTyping) {
        setTimeout(() => setTyping(`chat_${chatId}`, userId, false), 3000);
      }
    });
  };

  const unsubscribeFromChat = (chatId) => {
    wsService.unsubscribe(`/topic/chat/${chatId}`);
    wsService.unsubscribe(`/topic/chat/${chatId}/typing`);
  };

  const sendTyping = (chatId, isTypingNow) => {
    wsService.publish(`/app/chat/${chatId}/typing`, { typing: isTypingNow });
  };
  const [chatDetail, setChatDetail] = useState(null);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [showInfo, setShowInfo] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const messagesEndRef = useRef(null);
  const scrollContainerRef = useRef(null);
  const prevScrollHeightRef = useRef(0);

  const chatMessages = messages[activeChatId] || [];
  const typingKey = `chat_${activeChatId}`;
  const typingSet = typingUsers[typingKey] || new Set();
  const isTyping = typingSet.size > 0;

  useEffect(() => {
    if (!activeChatId) return;

    setPage(0);
    setHasMore(true);
    setLoading(true);

    getChatDetail(activeChatId)
      .then((res) => {
        setChatDetail(res.data);
        updateChat(activeChatId, {
          chatName: res.data.chatName,
          avatarUrl: res.data.avatarUrl,
          recipientOnline: res.data.recipientOnline,
        });
      })
      .catch(() => {});

    getMessages(activeChatId, 0, 30)
      .then((res) => {
        const data = res.data;
        const fetchedMsgs = Array.isArray(data) ? data : (data.content || []);
        const sorted = fetchedMsgs.reverse ? fetchedMsgs.slice().reverse() : fetchedMsgs;
        // Merge: keep only WS messages newer than the latest API message (avoids re-appending old paginated messages)
        const alreadyInStore = useChatStore.getState().messages[activeChatId] || [];
        const fetchedIds = new Set(sorted.map((m) => m.id));
        const latestFetchedTime = sorted.length > 0
          ? new Date(sorted[sorted.length - 1].createdDate).getTime()
          : 0;
        const wsOnlyMsgs = alreadyInStore.filter(
          (m) => !fetchedIds.has(m.id) && new Date(m.createdDate).getTime() > latestFetchedTime
        );
        setMessages(activeChatId, [...sorted, ...wsOnlyMsgs]);
        setHasMore(fetchedMsgs.length === 30);
        setTimeout(() => scrollToBottom(false), 100);
      })
      .catch(() => {})
      .finally(() => setLoading(false));

    markSeen(activeChatId).catch(() => {});
    clearUnread(activeChatId);
    subscribeToChat(activeChatId);

    return () => {
      unsubscribeFromChat(activeChatId);
    };
  }, [activeChatId]);

  // Re-fetch messages after WebSocket reconnect to catch any missed during disconnect
  useEffect(() => {
    if (!activeChatId) return;
    const refetch = () => {
      getMessages(activeChatId, 0, 30)
        .then((res) => {
          const data = res.data;
          const fetchedMsgs = Array.isArray(data) ? data : (data.content || []);
          const sorted = fetchedMsgs.slice().reverse();
          const existing = useChatStore.getState().messages[activeChatId] || [];
          const fetchedIds = new Set(sorted.map((m) => m.id));
          const latestTime = sorted.length > 0 ? new Date(sorted[sorted.length - 1].createdDate).getTime() : 0;
          const wsOnly = existing.filter((m) => !fetchedIds.has(m.id) && new Date(m.createdDate).getTime() > latestTime);
          setMessages(activeChatId, [...sorted, ...wsOnly]);
        })
        .catch(() => {});
    };
    wsService.onReconnect(refetch);
    return () => wsService.offReconnect(refetch);
  }, [activeChatId]);

  useEffect(() => {
    // Auto scroll to bottom only when new message arrives at the bottom
    const container = scrollContainerRef.current;
    if (!container) return;
    const isNearBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 200;
    if (isNearBottom) {
      scrollToBottom();
    }
  }, [chatMessages.length]);

  const scrollToBottom = (smooth = true) => {
    messagesEndRef.current?.scrollIntoView({ behavior: smooth ? 'smooth' : 'auto' });
  };

  const handleScroll = useCallback(async () => {
    const container = scrollContainerRef.current;
    if (!container || loadingMore || !hasMore) return;

    if (container.scrollTop < 100) {
      setLoadingMore(true);
      prevScrollHeightRef.current = container.scrollHeight;
      const nextPage = page + 1;

      try {
        const res = await getMessages(activeChatId, nextPage, 30);
        const data = res.data;
        const msgs = Array.isArray(data) ? data : (data.content || []);
        if (msgs.length === 0) {
          setHasMore(false);
        } else {
          prependMessages(activeChatId, msgs.slice().reverse());
          setPage(nextPage);
          setTimeout(() => {
            const newScrollHeight = container.scrollHeight;
            container.scrollTop = newScrollHeight - prevScrollHeightRef.current;
          }, 50);
        }
      } catch {}
      setLoadingMore(false);
    }
  }, [activeChatId, page, loadingMore, hasMore]);

  const handleSendText = async (content) => {
    try {
      const res = await sendMessage({ chatId: activeChatId, content, type: 'TEXT' });
      const msg = res.data;
      addMessage(activeChatId, msg);
      updateChatLastMessage(activeChatId, msg);
      setTimeout(() => scrollToBottom(), 50);
    } catch {
      toast.error('Không thể gửi tin nhắn');
    }
  };

  const handleSendMedia = async (files) => {
    const fileList = Array.isArray(files) ? files : [files];
    for (const file of fileList) {
      try {
        const res = await uploadMedia(activeChatId, file);
        addMessage(activeChatId, res.data);
        updateChatLastMessage(activeChatId, res.data);
      } catch {
        toast.error('Không thể gửi tệp');
      }
    }
    setTimeout(() => scrollToBottom(), 50);
  };

  const handleTyping = (isTypingNow) => {
    sendTyping(activeChatId, isTypingNow);
  };

  if (!activeChatId) {
    return (
      <div className="flex-1 flex items-center justify-center bg-gray-50">
        <div className="text-center text-gray-400">
          <div className="text-6xl mb-4">💬</div>
          <p className="text-lg font-medium">Chọn một cuộc trò chuyện</p>
          <p className="text-sm mt-1">để bắt đầu nhắn tin</p>
        </div>
      </div>
    );
  }

  const recipientName = chatDetail?.chatName || 'Người dùng';

  return (
    <div className="flex-1 flex h-full overflow-hidden">
    <div className="flex-1 flex flex-col h-full bg-gray-50 min-w-0">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 bg-white border-b border-gray-100 shadow-sm">
        <button
          className="md:hidden p-1 hover:bg-gray-100 rounded-full"
          onClick={() => setActiveChatId(null)}
        >
          <ChevronLeft size={20} />
        </button>
        <Avatar
          src={chatDetail?.avatarUrl}
          name={recipientName}
          size={40}
          online={chatDetail?.recipientOnline}
        />
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-gray-800 truncate">{recipientName}</h3>
          <p className="text-xs text-gray-400">
            {chatDetail?.recipientOnline
              ? 'Đang hoạt động'
              : chatDetail?.recipientLastSeenText || 'Không hoạt động'}
          </p>
        </div>
        <div className="flex items-center gap-1">
          <button className="w-9 h-9 flex items-center justify-center rounded-full hover:bg-gray-100 transition-colors" title="Gọi thoại">
            <Phone size={18} className="text-gray-600" />
          </button>
          <button className="w-9 h-9 flex items-center justify-center rounded-full hover:bg-gray-100 transition-colors" title="Gọi video">
            <Video size={18} className="text-gray-600" />
          </button>
          <button
            onClick={() => setShowInfo((v) => !v)}
            className={`w-9 h-9 flex items-center justify-center rounded-full transition-colors ${showInfo ? 'bg-blue-50 text-blue-500' : 'hover:bg-gray-100 text-gray-600'}`}
            title="Thông tin"
          >
            <Info size={18} />
          </button>
        </div>
      </div>

      {/* Messages */}
      <div
        ref={scrollContainerRef}
        onScroll={handleScroll}
        className="flex-1 overflow-y-auto py-4"
      >
        {loadingMore && (
          <div className="flex justify-center py-2">
            <div className="w-5 h-5 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
          </div>
        )}
        {loading ? (
          <div className="flex justify-center py-8">
            <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          chatMessages.map((msg) => (
            <MessageBubble
              key={msg.id}
              message={msg}
              chatId={activeChatId}
            />
          ))
        )}
        {isTyping && <TypingIndicator />}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <MessageInput
        onSendText={handleSendText}
        onSendMedia={handleSendMedia}
        onTyping={handleTyping}
        placeholder="Nhập tin nhắn..."
      />
    </div>

    {showInfo && (
      <ChatInfoPanel chatDetail={chatDetail} onClose={() => setShowInfo(false)} />
    )}
    </div>
  );
}
