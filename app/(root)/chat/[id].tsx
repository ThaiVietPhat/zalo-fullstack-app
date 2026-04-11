// app/(root)/chat/[id].tsx — đồng bộ với MessageDto.java mới
// Key changes: deleted thay vì recalled, state thay vì seen, receiverId thay vì seen boolean
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
import { useAuth } from '@/context/AuthContext';
import { getAvatarUrl } from '@/lib/utils';

const ChatScreen = () => {
  const { id, name, isGroup } = useLocalSearchParams<{ id: string; name?: string; isGroup?: string }>();
  const isGroupBool = isGroup === 'true';
  const { user } = useAuth();
  const flatListRef = useRef<FlatList<any>>(null);

  // Lấy chi tiết chat để có chatName nếu không có params
  const { data: chat } = useChatById(id);
  const { data: messages, isLoading } = useMessages(id);
  const { mutate: markSeen } = useMarkSeen();

  useEffect(() => {
    if (id) markSeen(id);
    // Tự động cuộn xuống cuối khi có tin nhắn mới
    setTimeout(() => {
      flatListRef.current?.scrollToEnd({ animated: true });
    }, 300);
  }, [id, messages]);

  // chatName: ưu tiên param truyền vào, sau đó dùng chatName từ ChatDto
  const chatName = (name as string) || chat?.chatName || "Chat";
  const myId = user?.id || "";

  if (isLoading) {
    return (
      <View className="flex-1 bg-[#E2E9F1]">
        <ChatHeader name={chatName} avatarUrl={chat?.avatarUrl} online={chat?.recipientOnline} />
        <View className="flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#0068FF" />
        </View>
      </View>
    );
  }

  // Chuyển MessageDto → format mà MessageItem cần
  const formattedMessages = (messages || []).map((msg) => ({
    id: msg.id || `${msg.senderId}-${msg.createdAt}`,
    senderId: msg.senderId || "",
    chatId: id,
    // deleted = true → "Tin nhắn đã bị xóa"
    text: msg.deleted ? "Tin nhắn đã bị xóa" : msg.content,
    image: (!msg.deleted && msg.mediaUrl) ? msg.mediaUrl : undefined,
    time: msg.createdAt
      ? new Date(msg.createdAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })
      : "",
    type: (msg.type === "IMAGE" ? "IMAGE" : "TEXT") as "TEXT" | "IMAGE",
    state: msg.state,
    reactions: msg.reactions,
    avatar: msg.senderId === user?.id
      ? getAvatarUrl(user?.name || "Me", user?.avatar)
      : (isGroupBool ? undefined : getAvatarUrl(chat?.chatName || "Other", chat?.avatarUrl)),
    senderName: msg.senderId === user?.id ? user?.name : (isGroupBool ? "User" : chat?.chatName),
  }));

  return (
    <View className="flex-1 bg-[#E2E9F1]">
      <ChatHeader
        name={chatName}
        avatarUrl={chat?.avatarUrl}
        online={chat?.recipientOnline}
        lastSeenText={chat?.recipientLastSeenText}
      />

      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : undefined}
        className="flex-1"
        keyboardVerticalOffset={Platform.OS === "ios" ? 90 : 0}
      >
        {formattedMessages.length === 0 ? (
          <View className="flex-1 justify-center items-center">
            <Text className="text-gray-400 font-JakartaMedium">Bắt đầu cuộc trò chuyện</Text>
          </View>
        ) : (
          <FlatList
            ref={flatListRef}
            data={[...formattedMessages].reverse()}
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => (
              <MessageItem
                item={item}
                isMe={item.senderId === myId}
                isGroup={isGroupBool}
              />
            )}
            className="flex-1 pt-4"
            showsVerticalScrollIndicator={false}
            contentContainerStyle={{ paddingVertical: 16 }}
            onContentSizeChange={() => flatListRef.current?.scrollToEnd({ animated: true })}
          />
        )}

        <ChatInput chatId={id} isGroup={isGroupBool} />
      </KeyboardAvoidingView>
    </View>
  );
};

export default ChatScreen;
