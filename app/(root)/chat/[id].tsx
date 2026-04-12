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
import { useGroupById, useGroupMessages } from '@/hooks/useGroup';
import { useAuth } from '@/context/AuthContext';
import { getAvatarUrl } from '@/lib/utils';

const ChatScreen = () => {
  const { id, name, isGroup } = useLocalSearchParams<{ id: string; name?: string; isGroup?: string }>();
  const isGroupBool = isGroup === 'true';
  const { user } = useAuth();
  const flatListRef = useRef<FlatList<any>>(null);

  // Lấy chi tiết chat để có chatName nếu không có params
  // Lấy chi tiết chat (1-1 hoặc Nhóm)
  const { data: chat } = useChatById(!isGroupBool ? id : null);
  const { data: group } = useGroupById(isGroupBool ? id : null);

  // Lấy tin nhắn (1-1 hoặc Nhóm) — polling mỗi 3 giây để tự động cập nhật
  const { data: soloMessages, isLoading: loadingSolo } = useMessages(!isGroupBool ? id : null);
  const { data: groupMessages, isLoading: loadingGroup } = useGroupMessages(isGroupBool ? id : null, 0, 50);

  const messages = isGroupBool ? groupMessages : soloMessages;
  const isLoading = isGroupBool ? loadingGroup : loadingSolo;

  const { mutate: markSeen } = useMarkSeen();

  useEffect(() => {
    // Chỉ markSeen cho chat 1-1 (BE hiện tại chưa có API markSeen cho Group)
    if (id && !isGroupBool) markSeen(id);
  }, [id, messages]);

  // chatName: ưu tiên param truyền vào, sau đó dùng chatName từ ChatDto
  // chatName: ưu tiên param truyền vào, sau đó dùng chatName từ ChatDto hoặc GroupDto
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

  // Chuyển MessageDto → format mà MessageItem cần
  const formattedMessages = (messages || []).map((msg) => ({
    id: msg.id || `${msg.senderId}-${msg.createdAt}`,
    senderId: msg.senderId || "",
    chatId: id,
    // deleted = true → "Tin nhắn đã bị xóa"
    text: msg.deleted ? "Tin nhắn đã bị xóa" : msg.content,
    image: (!msg.deleted && msg.mediaUrl) ? msg.mediaUrl : undefined,
    time: (msg.createdAt || msg.createdDate)
      ? new Date((msg.createdAt || msg.createdDate)!).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })
      : "",
    type: (msg.type === "IMAGE" ? "IMAGE" : "TEXT") as "TEXT" | "IMAGE",
    state: msg.state,
    reactions: msg.reactions,
    avatar: msg.senderId === user?.id
      ? getAvatarUrl(user?.name || "Me", user?.avatar)
      : (isGroupBool ? getAvatarUrl(msg.senderName || "User", undefined) : getAvatarUrl(chat?.chatName || "Other", chat?.avatarUrl)),
    senderName: msg.senderId === user?.id ? user?.name : (isGroupBool ? (msg.senderName || "User") : (chat?.chatName || "Người dùng")),
  }));

  // BE không đồng bộ: Private trả về DESC (Newest first), Group trả về ASC (Oldest first)
  // Để dùng inverted FlatList (Index 0 ở đáy), ta cần mảng luôn là DESC (Newest first)
  const finalMessages = isGroupBool ? [...formattedMessages].reverse() : formattedMessages;

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
            inverted // Đảo ngược danh sách: index 0 ở dưới cùng (tin nhắn mới nhất)
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
