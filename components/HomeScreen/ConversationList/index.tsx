// components/HomeScreen/ConversationList/index.tsx
import React from "react";
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  Image,
  StyleSheet,
} from "react-native";
import { router } from "expo-router";
import { useChats } from "@/hooks/useChat";
import { ChatDto } from "@/api/chat";
import { getAvatarUrl } from "@/lib/utils";

const ConversationList = () => {
  const { data: chats, isLoading, refetch, isRefetching } = useChats();

  const getPreview = (chat: ChatDto): string => {
    if (!chat.lastMessage) return "Bắt đầu trò chuyện";
    const { lastMessage, lastMessageType } = chat;
    if (lastMessageType === "IMAGE") return "🖼 Hình ảnh";
    if (lastMessageType === "VIDEO") return "🎥 Video";
    if (lastMessageType === "FILE") return "📎 File";
    if (lastMessageType === "AUDIO" || lastMessageType === "VOICE") return "🎵 Âm thanh";
    return lastMessage;
  };

  const formatTime = (iso?: string): string => {
    if (!iso) return "";
    const d = new Date(iso);
    const now = new Date();
    const diffDays = Math.floor((now.getTime() - d.getTime()) / (1000 * 60 * 60 * 24));
    if (diffDays === 0) return d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
    if (diffDays === 1) return "Hôm qua";
    return d.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" });
  };

  if (isLoading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#0068FF" />
      </View>
    );
  }

  if (!chats || chats.length === 0) {
    return (
      <View style={styles.centered}>
        <Text style={styles.emptyTitle}>Chưa có tin nhắn nào</Text>
        <Text style={styles.emptySubtitle}>Mở tab Danh bạ để bắt đầu trò chuyện</Text>
      </View>
    );
  }

  return (
    <FlatList
      data={chats}
      keyExtractor={(item) => item.id}
      onRefresh={refetch}
      refreshing={isRefetching}
      contentContainerStyle={{ paddingBottom: 12 }}
      renderItem={({ item }) => {
        // ChatDto.java: chatName là tên người kia, avatarUrl, recipientOnline
        const name = item.chatName || item.recipientEmail || "Người dùng";
        const avatarUrl = getAvatarUrl(name, item.avatarUrl);
        const preview = getPreview(item);
        const time = formatTime(item.lastMessageTime);
        const hasUnread = (item.unreadCount || 0) > 0;

        return (
          <TouchableOpacity
            style={styles.item}
            onPress={() =>
              router.push({
                pathname: "/(root)/chat/[id]",
                params: { id: item.id, name: item.chatName || "" },
              })
            }
            activeOpacity={0.7}
          >
            {/* Avatar */}
            <View style={styles.avatarWrapper}>
              <Image source={{ uri: avatarUrl }} style={styles.avatar} />
              {item.recipientOnline && <View style={styles.onlineDot} />}
            </View>

            {/* Content */}
            <View style={styles.content}>
              <View style={styles.topRow}>
                <Text style={[styles.name, hasUnread && styles.nameBold]} numberOfLines={1}>
                  {name}
                </Text>
                <Text style={[styles.time, hasUnread && styles.timeBlue]}>{time}</Text>
              </View>
              <View style={styles.bottomRow}>
                <Text style={[styles.preview, hasUnread && styles.previewBold]} numberOfLines={1}>
                  {preview}
                </Text>
                {hasUnread && (
                  <View style={styles.badge}>
                    <Text style={styles.badgeText}>
                      {item.unreadCount > 99 ? "99+" : item.unreadCount}
                    </Text>
                  </View>
                )}
              </View>
              {/* recipientLastSeenText */}
              {!item.recipientOnline && item.recipientLastSeenText ? (
                <Text style={styles.lastSeen}>{item.recipientLastSeenText}</Text>
              ) : null}
            </View>
          </TouchableOpacity>
        );
      }}
    />
  );
};

const styles = StyleSheet.create({
  centered: { flex: 1, justifyContent: "center", alignItems: "center", paddingTop: 60 },
  emptyTitle: { fontSize: 16, fontFamily: "Jakarta-Bold", color: "#9CA3AF", marginBottom: 8 },
  emptySubtitle: { fontSize: 14, fontFamily: "Jakarta-Medium", color: "#D1D5DB", textAlign: "center", paddingHorizontal: 32 },
  item: { flexDirection: "row", alignItems: "center", paddingHorizontal: 16, paddingVertical: 12, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: "#F3F4F6" },
  avatarWrapper: { position: "relative", marginRight: 12 },
  avatar: { width: 52, height: 52, borderRadius: 26, backgroundColor: "#E5E7EB" },
  onlineDot: { position: "absolute", bottom: 1, right: 1, width: 12, height: 12, borderRadius: 6, backgroundColor: "#22C55E", borderWidth: 2, borderColor: "white" },
  content: { flex: 1 },
  topRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 3 },
  name: { fontSize: 15, fontFamily: "Jakarta-Medium", color: "#1F2937", flex: 1, marginRight: 8 },
  nameBold: { fontFamily: "Jakarta-Bold" },
  time: { fontSize: 12, fontFamily: "Jakarta-Medium", color: "#9CA3AF" },
  timeBlue: { color: "#0068FF" },
  bottomRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  preview: { fontSize: 13, fontFamily: "Jakarta", color: "#6B7280", flex: 1 },
  previewBold: { fontFamily: "Jakarta-Medium", color: "#374151" },
  badge: { backgroundColor: "#0068FF", borderRadius: 10, minWidth: 20, height: 20, justifyContent: "center", alignItems: "center", paddingHorizontal: 5, marginLeft: 8 },
  badgeText: { color: "white", fontSize: 11, fontFamily: "Jakarta-Bold" },
  lastSeen: { fontSize: 11, color: "#94A3B8", fontFamily: "Jakarta-Medium", marginTop: 2 },
});

export default ConversationList;
