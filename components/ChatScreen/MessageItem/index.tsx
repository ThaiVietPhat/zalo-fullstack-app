// components/ChatScreen/MessageItem/index.tsx
import React from 'react';
import { View, Text, Image, TouchableOpacity, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { MessageState } from '@/types/type';
import { getImageUrl } from '@/lib/utils';

interface Message {
  id: string;
  senderId: string;
  text?: string;
  image?: string;
  time: string;
  type: 'TEXT' | 'IMAGE';
  state?: MessageState;
  reactions?: Array<{ emoji: string; userId: string }>;
}

const MessageItem = ({ item, isMe }: { item: Message; isMe: boolean }) => {
  const isDeleted = item.text === 'Tin nhắn đã bị xóa';

  // Icon trạng thái gửi (Seen, Delivered)
  const StateIcon = () => {
    if (!isMe || !item.state || isDeleted) return null;
    if (item.state === 'SEEN') return <Ionicons name="checkmark-done" size={14} color="#0068FF" />;
    if (item.state === 'DELIVERED') return <Ionicons name="checkmark-done" size={14} color="#94A3B8" />;
    return <Ionicons name="checkmark" size={14} color="#94A3B8" />;
  };

  return (
    <View className={`flex-row mb-3 ${isMe ? 'justify-end' : 'justify-start'} px-3`}>
      {/* Avatar người khác */}
      {!isMe && (
        <View className="mr-2 self-end pb-1">
          <View className="w-8 h-8 rounded-full bg-blue-100 items-center justify-center">
            <Ionicons name="person" size={18} color="#0068FF" />
          </View>
        </View>
      )}

      <View className={`${isMe ? 'items-end' : 'items-start'} max-w-[80%]`}>
        {/* Bong bóng tin nhắn */}
        <View
          style={[
            styles.bubble,
            isMe ? styles.myBubble : styles.otherBubble,
            isDeleted && styles.deletedBubble
          ]}
        >
          {/* Nội dung hình ảnh */}
          {item.type === 'IMAGE' && item.image && !isDeleted ? (
            <View className="mb-1 rounded-xl overflow-hidden">
              <Image
                source={{ uri: getImageUrl(item.image) }}
                className="w-56 h-72"
                resizeMode="cover"
              />
            </View>
          ) : (
            <Text
              style={[
                styles.messageText,
                isDeleted && styles.deletedText,
                isMe ? styles.myText : styles.otherText
              ]}
            >
              {item.text}
            </Text>
          )}

          {/* Hàng chứa thời gian và tim bên dưới cùng của bong bóng */}
          {!isDeleted && (
            <View style={styles.footerRow}>
              {/* Thời gian ở phía dưới trái */}
              <View style={styles.timeWrapper}>
                <Text style={[styles.timeText, isMe ? styles.myTime : styles.otherTime]}>
                  {item.time}
                </Text>
                <StateIcon />
              </View>

              {/* Icon tim ở phía dưới phải */}
              <TouchableOpacity className="ml-2">
                <Ionicons 
                  name={item.reactions?.length ? "heart" : "heart-outline"} 
                  size={16} 
                  color={isMe ? "#0068FF" : "#8E8E93"} 
                />
              </TouchableOpacity>
            </View>
          )}
        </View>

        {/* Reaction badge (nếu có biểu cảm khác) */}
        {item.reactions && item.reactions.length > 0 && (
          <View className="flex-row mt-1 gap-1 px-2">
            {item.reactions.map((r, i) => (
              <Text key={i} style={{ fontSize: 14 }}>{r.emoji}</Text>
            ))}
          </View>
        )}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  bubble: {
    paddingHorizontal: 12,
    paddingTop: 8,
    paddingBottom: 4,
    borderRadius: 18,
    borderWidth: 0.5,
    minWidth: 80,
  },
  myBubble: {
    backgroundColor: '#EAF6FF',
    borderColor: '#D5E9F7',
    borderBottomRightRadius: 4,
  },
  otherBubble: {
    backgroundColor: '#FFFFFF',
    borderColor: '#E1E6E9',
    borderBottomLeftRadius: 4,
  },
  deletedBubble: {
    backgroundColor: '#F3F4F6',
    borderColor: '#E5E7EB',
  },
  messageText: {
    fontSize: 16,
    fontFamily: 'Jakarta',
    lineHeight: 22,
  },
  myText: { color: '#1F2937' },
  otherText: { color: '#1F2937' },
  deletedText: {
    color: '#9CA3AF',
    fontStyle: 'italic',
  },
  footerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 4,
  },
  timeWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  timeText: {
    fontSize: 10,
    fontFamily: 'Jakarta-Medium',
  },
  myTime: { color: '#6B7280' },
  otherTime: { color: '#94A3B8' },
});

export default MessageItem;
