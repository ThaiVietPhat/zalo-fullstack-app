// app/(root)/chat/[id].tsx — đồng bộ với MessageDto.java mới
import React, { useRef, useEffect } from 'react';
import {
  View,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  ActivityIndicator,
  Text,
} from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import ChatHeader from '@/components/ChatScreen/ChatHeader';
import MessageItem from '@/components/ChatScreen/MessageItem';
import ChatInput from '@/components/ChatScreen/ChatInput';
import { useMessages, useMarkSeen } from '@/hooks/useMessages';
import { useChatById } from '@/hooks/useChat';
import { useGroupById, useGroupMessages } from '@/hooks/useGroup';
import { useAuth } from '@/context/AuthContext';
import { getAvatarUrl, getImageUrl } from '@/lib/utils';
import { useSocket } from '@/context/SocketContext';
import { useQueryClient } from '@tanstack/react-query';

const ChatScreen = () => {
  const { id, name, isGroup } = useLocalSearchParams<{ id: string; name?: string; isGroup?: string }>();
  const isGroupBool = isGroup === 'true';
  const { user } = useAuth();
  const flatListRef = useRef<FlatList<any>>(null);
  const { subscribeToChat, setActiveChat } = useSocket(); // SỬA: Đã thêm setActiveChat ở đây
  const queryClient = useQueryClient();

  const { data: chat } = useChatById(!isGroupBool ? id : null);
  const { data: group } = useGroupById(isGroupBool ? id : null);
  const { data: soloMessages, isLoading: loadingSolo } = useMessages(!isGroupBool ? id : null);
  const { data: groupMessages, isLoading: loadingGroup } = useGroupMessages(isGroupBool ? id : null, 0, 50);

  const messages = isGroupBool ? groupMessages : soloMessages;
  const isLoading = isGroupBool ? loadingGroup : loadingSolo;

  const { mutate: markSeen } = useMarkSeen();

  useEffect(() => {
    if (id) {
      // 1. Đánh dấu đang xem phòng này
      setActiveChat(id);

      // 2. Reset số đỏ ở Home ngay lập tức
      const listKey = isGroupBool ? ['groups'] : ['chats'];
      queryClient.setQueryData(listKey, (old: any[] | undefined) => {
        if (!old) return old;
        return old.map((item: any) =>
          item.id === id ? { ...item, unreadCount: 0 } : item
        );
      });

      // 3. Thông báo server (1-1)
      if (!isGroupBool) markSeen(id);

      return () => {
        setActiveChat(null); // Thoát phòng
      };
    }
  }, [id, isGroupBool]);

  // Cập nhật Cache từ Socket đã được xử lý tập trung bên SocketContext.tsx

  const chatName = (name as string) || (isGroupBool ? group?.name : chat?.chatName) || "Chat";
  const avatarUrl = isGroupBool ? group?.avatarUrl : chat?.avatarUrl;
  const isOnline = isGroupBool ? false : chat?.recipientOnline;
  const myId = user?.id || "";

  if (isLoading) {
    return (
      <View className="flex-1 bg-[#E2E9F1]">
        <ChatHeader name={chatName} avatarUrl={avatarUrl} online={isOnline} />
        <View className="flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#0068FF" />
        </View>
      </View>
    );
  }

  const formattedMessages = (messages as any[] || []).map((msg: any, idx: number) => {
    const sender = isGroupBool ? (group as any)?.members?.find((m: any) => (m.userId || m.id) === msg.senderId) : null;
    const isMe = msg.senderId === myId;
    const messageText = msg.text || ((!msg.deleted && !msg.mediaUrl) ? msg.content : (msg.deleted ? (msg.senderId === user?.id ? "Tin nhắn đã được thu hồi" : "Tin nhắn đã bị xóa") : ""));

    return {
      id: msg.id || `${idx}`,
      chatId: id,
      senderId: msg.senderId || "",
      text: messageText,
      image: (!msg.deleted && msg.mediaUrl) ? msg.mediaUrl : undefined,
      time: (msg.createdDate || msg.createdAt) ? new Date(msg.createdDate || msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : "00:00",
      type: msg.type || msg.messageType || 'TEXT',
      state: msg.state || (msg.id ? 'DELIVERED' : 'SENT'),
      avatar: sender?.avatarUrl ? getImageUrl(sender.avatarUrl) : (isGroupBool ? getAvatarUrl(msg.senderName || "User", undefined) : getAvatarUrl(chat?.chatName || "Other", chat?.avatarUrl)),
      senderName: sender?.fullName || msg.senderName || (msg.senderId === user?.id ? user?.name : (isGroupBool ? "User" : (chat?.chatName || "Người dùng"))),
      reactions: msg.reactions || []
    };
  });

  const finalMessages = formattedMessages;

  return (
    <View className="flex-1 bg-[#E2E9F1]">
      <ChatHeader
        name={chatName}
        avatarUrl={avatarUrl}
        online={isOnline}
        lastSeenText={isGroupBool ? undefined : chat?.recipientLastSeenText}
        isGroup={isGroupBool}
        groupId={isGroupBool ? id : undefined}
      />

      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : "height"}
        className="flex-1"
        keyboardVerticalOffset={0}
      >
        {finalMessages.length === 0 ? (
          <View className="flex-1 justify-center items-center">
            <Text className="text-gray-400 font-JakartaMedium">Bắt đầu cuộc trò chuyện</Text>
          </View>
        ) : (
          <FlatList
            ref={flatListRef}
            data={finalMessages}
            inverted
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => (
              <MessageItem
                item={item}
                isMe={item.senderId === myId}
                isGroup={isGroupBool}
              />
            )}
            className="flex-1"
            showsVerticalScrollIndicator={false}
            contentContainerStyle={{ paddingVertical: 16, paddingHorizontal: 0 }}
          />
        )}
        <ChatInput chatId={id} isGroup={isGroupBool} />
      </KeyboardAvoidingView>
    </View>
  );
};

export default ChatScreen;
